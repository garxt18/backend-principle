package com.backendprinciple.playground;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@EnabledIf("com.backendprinciple.playground.AbstractIntegrationTest#databaseAvailable")
class ApiFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    record Session(String token, Cookie refresh) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Session register(String name) throws Exception {
        String email = name + "-" + UUID.randomUUID() + "@example.com";
        MvcResult res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","displayName":"%s"}""".formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return new Session(body(res).get("accessToken").asText(), res.getResponse().getCookie("pg_refresh"));
    }

    private JsonNode body(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void registerLoginAndProtectedEndpoints() throws Exception {
        Session s = register("asha");
        assertThat(s.refresh().isHttpOnly()).isTrue();

        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("asha"))
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(get("/api/admin/stats").header("Authorization", s.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void refreshTokensRotateAndTwoTabsRefreshingAtOnceStayLoggedIn() throws Exception {
        Session s = register("ravi");
        MvcResult first = mvc.perform(post("/api/auth/refresh").cookie(s.refresh())).andExpect(status().isOk()).andReturn();
        Cookie rotated = first.getResponse().getCookie("pg_refresh");
        assertThat(rotated.getValue()).isNotEqualTo(s.refresh().getValue());

        // A second tab sends the same (already rotated) cookie a moment later: a race, not theft.
        MvcResult second = mvc.perform(post("/api/auth/refresh").cookie(s.refresh())).andExpect(status().isOk()).andReturn();
        Cookie secondTab = second.getResponse().getCookie("pg_refresh");
        mvc.perform(post("/api/auth/refresh").cookie(rotated)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").cookie(secondTab)).andExpect(status().isOk());

        // After logout nothing from that session works any more - not even inside the grace window.
        mvc.perform(post("/api/auth/logout").cookie(rotated)).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").cookie(s.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(secondTab)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWorksCaseInsensitivelyAndWrongPasswordsAreRejected() throws Exception {
        // Without GOOGLE_CLIENT_ID/SECRET the Google button is hidden and its URL is not an OAuth endpoint.
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.google").value(false));
        String email = "Case-" + UUID.randomUUID() + "@Example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"password123\",\"displayName\":\"Case\"}".formatted(email)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  %s \",\"password\":\"password123\"}".formatted(email.toLowerCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        // Same email twice is a clean 409, not a 500.
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"displayName\":\"Case\"}".formatted(email.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void roadmapProgressAndPlanner() throws Exception {
        Session s = register("meera");
        JsonNode roadmap = body(mvc.perform(get("/api/roadmap")).andExpect(status().isOk()).andReturn());
        assertThat(roadmap.size()).isGreaterThanOrEqualTo(16);
        long firstTopic = roadmap.get(0).get("topics").get(0).get("id").asLong();

        mvc.perform(put("/api/progress/topics/" + firstTopic).header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"confidence\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists());
        mvc.perform(put("/api/progress/topics/99999999").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/sessions").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"minutes\":50,\"topicId\":" + firstTopic + "}")).andExpect(status().isCreated());

        mvc.perform(get("/api/progress").header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicsDone").value(1))
                .andExpect(jsonPath("$.currentStreakDays").value(1));

        // Creating a second plan archives the first - there is only ever one active plan.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/plans").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hoursPerWeek\":20,\"skipCompleted\":true}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currentWeek").value(1));
        }
        JsonNode plan = body(mvc.perform(get("/api/plans/current").header("Authorization", s.bearer()))
                .andExpect(status().isOk()).andReturn());
        assertThat(plan.get("totalWeeks").asInt()).isBetween(20, 40);
        assertThat(plan.get("weeks").get(0).get("items").get(0).get("topicId").asLong()).isNotEqualTo(firstTopic);
    }

    @Test
    void planlyFinishesALevelByADeadlineAndStaysInSyncWithTheRoadmap() throws Exception {
        Session s = register("neha");
        java.time.LocalDate today = java.time.LocalDate.now();
        String sprint = """
                {"name":"Java Basics sprint","pace":"DEADLINE","targetEndDate":"%s","levelNumbers":[0],
                 "studyDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]}"""
                .formatted(today.plusDays(6));

        // Preview first: nothing is saved, but hours per day and the finish date are known.
        JsonNode preview = body(mvc.perform(post("/api/plans/preview").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(sprint)).andExpect(status().isOk()).andReturn());
        assertThat(preview.get("topics").asInt()).isEqualTo(22);
        assertThat(java.time.LocalDate.parse(preview.get("endDate").asText())).isBeforeOrEqualTo(today.plusDays(6));
        assertThat(preview.get("hoursPerDay").decimalValue()).isBetween(new java.math.BigDecimal("5"), new java.math.BigDecimal("9"));
        mvc.perform(get("/api/plans/current").header("Authorization", s.bearer())).andExpect(status().isNotFound());

        JsonNode plan = body(mvc.perform(post("/api/plans").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(sprint)).andExpect(status().isCreated()).andReturn());
        assertThat(plan.get("paceMode").asText()).isEqualTo("DEADLINE");
        assertThat(plan.get("topicsTotal").asInt()).isEqualTo(22);
        JsonNode todayItems = plan.get("today").get("items");
        assertThat(todayItems.size()).isPositive();
        assertThat(todayItems.get(0).get("lectureNumber").asInt()).isEqualTo(1);

        // Ticking the topic in the roadmap shows up in the plan (and the other way round - it is one record).
        long topicId = todayItems.get(0).get("topicId").asLong();
        mvc.perform(put("/api/progress/topics/" + topicId).header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/plans/current").header("Authorization", s.bearer()))
                .andExpect(jsonPath("$.today.items[0].done").value(true))
                .andExpect(jsonPath("$.topicsDone").value(1));

        // Re-planning the rest keeps finished work and schedules only what is left.
        mvc.perform(post("/api/plans/current/replan").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"keepDeadline\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicsTotal").value(22))
                .andExpect(jsonPath("$.topicsDone").value(1));

        // Pace mode: 2 hours a day.
        mvc.perform(post("/api/plans").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pace\":\"HOURS\",\"hoursPerDay\":2,\"levelNumbers\":[3]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hoursPerDay").value(2.0));

        // An impossible deadline is refused with a clear message instead of a 20-hour day.
        mvc.perform(post("/api/plans").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pace\":\"DEADLINE\",\"targetEndDate\":\"%s\",\"levelNumbers\":[2,3],\"studyDays\":[\"%s\"]}"
                                .formatted(today, today.getDayOfWeek())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("hours per study day")));
    }

    @Test
    void labUploadIsPrivateToItsOwner() throws Exception {
        Session owner = register("kiran");
        Session other = register("dev");

        MockMultipartFile zip = new MockMultipartFile("file", "shop.zip", "application/zip", zip());
        JsonNode created = body(mvc.perform(multipart("/api/lab/projects").file(zip).param("name", "Shop")
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isCreated()).andReturn());
        String projectId = created.get("project").get("id").asText();

        JsonNode detail = body(mvc.perform(get("/api/lab/projects/" + projectId).header("Authorization", owner.bearer()))
                .andExpect(status().isOk()).andReturn());
        assertThat(detail.get("files").get(0).get("path").asText()).isEqualTo("pom.xml");
        String entityFile = detail.get("files").get(1).get("id").asText();

        mvc.perform(get("/api/lab/files/" + entityFile + "/lines/1/explain").header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes[0].term").value("@Entity"));
        mvc.perform(put("/api/lab/files/" + entityFile + "/progress").header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"linesCompleted\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linesCompleted").value(2)); // clamped to the file's line count

        // Another learner cannot see, read or delete it - and gets 404, not 403, so ids cannot be probed.
        mvc.perform(get("/api/lab/projects/" + projectId).header("Authorization", other.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/lab/files/" + entityFile).header("Authorization", other.bearer())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/lab/projects/" + projectId).header("Authorization", other.bearer())).andExpect(status().isNotFound());

        // Templates are visible to everyone.
        JsonNode list = body(mvc.perform(get("/api/lab/projects").header("Authorization", other.bearer())).andReturn());
        assertThat(list.valueStream().anyMatch(p -> p.get("template").asBoolean())).isTrue();

        mvc.perform(delete("/api/lab/projects/" + projectId).header("Authorization", owner.bearer())).andExpect(status().isNoContent());
    }

    @Test
    void javaAndSpringLevelsAreLectureByLecturePlaylists() throws Exception {
        JsonNode roadmap = body(mvc.perform(get("/api/roadmap")).andExpect(status().isOk()).andReturn());
        JsonNode basics = roadmap.get(0);
        assertThat(basics.get("title").asText()).isEqualTo("Java Basics");
        assertThat(basics.get("playlist").get("url").asText()).contains("PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p");
        assertThat(basics.get("resources")).hasSize(1);
        JsonNode lecture1 = basics.get("topics").get(0);
        assertThat(lecture1.get("lectureNumber").asInt()).isEqualTo(1);
        assertThat(lecture1.get("videoUrl").asText()).startsWith("https://www.youtube.com/watch?v=");

        JsonNode spring = roadmap.valueStream().filter(l -> l.get("slug").asText().equals("spring-boot"))
                .findFirst().orElseThrow();
        assertThat(spring.get("playlist").get("url").asText()).contains("PLEYgx5hMdopw");
        assertThat(spring.get("topics").valueStream().filter(t -> !t.get("lectureNumber").isNull()).count())
                .isEqualTo(40);

        // No resource anywhere sends people to a YouTube search page.
        assertThat(roadmap.valueStream().flatMap(l -> l.get("resources").valueStream())
                .noneMatch(r -> r.get("url").asText().contains("results?search_query"))).isTrue();
    }

    @Test
    void learnersFollowOneResourcePerLevelAndAttachTheirOwnLinks() throws Exception {
        Session s = register("tara");
        JsonNode roadmap = body(mvc.perform(get("/api/roadmap")).andReturn());
        JsonNode docker = roadmap.valueStream().filter(l -> l.get("slug").asText().equals("docker-cloud"))
                .findFirst().orElseThrow();
        long levelId = docker.get("id").asLong();
        long resourceId = docker.get("resources").get(1).get("id").asLong();
        long topicId = roadmap.get(0).get("topics").get(9).get("id").asLong(); // Java lecture 10

        mvc.perform(put("/api/me/resources/follow/" + levelId).header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resourceId\":" + resourceId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(resourceId));
        // A resource from another level cannot be followed here.
        long otherLevelResource = roadmap.get(1).get("resources").get(0).get("id").asLong();
        mvc.perform(put("/api/me/resources/follow/" + levelId).header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resourceId\":" + otherLevelResource + "}"))
                .andExpect(status().isBadRequest());

        // Own link on one lecture: the level is derived from the topic.
        JsonNode link = body(mvc.perform(post("/api/me/resources/links").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"topicId":%d,"title":"Lecture 10 video","url":"https://www.youtube.com/watch?v=abc"}"""
                                .formatted(topicId)))
                .andExpect(status().isCreated()).andReturn());
        assertThat(link.get("levelId").asLong()).isEqualTo(roadmap.get(0).get("id").asLong());
        // Only http(s) links are accepted.
        mvc.perform(post("/api/me/resources/links").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"levelId\":" + levelId + ",\"title\":\"x\",\"url\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        // Own level link can be followed too.
        long ownId = body(mvc.perform(post("/api/me/resources/links").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"levelId\":" + levelId + ",\"title\":\"My Docker playlist\",\"url\":\"https://www.youtube.com/playlist?list=PLx\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(put("/api/me/resources/follow/" + levelId).header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userResourceId\":" + ownId + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me/resources").header("Authorization", s.bearer()))
                .andExpect(jsonPath("$.links.length()").value(2))
                .andExpect(jsonPath("$.choices[0].userResourceId").value(ownId));

        // Links are private: another learner can neither see nor delete them.
        Session other = register("uma");
        mvc.perform(get("/api/me/resources").header("Authorization", other.bearer()))
                .andExpect(jsonPath("$.links.length()").value(0));
        mvc.perform(delete("/api/me/resources/links/" + ownId).header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());

        // Deleting the followed link also clears the choice (ON DELETE CASCADE).
        mvc.perform(delete("/api/me/resources/links/" + ownId).header("Authorization", s.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me/resources").header("Authorization", s.bearer()))
                .andExpect(jsonPath("$.choices.length()").value(0));
    }

    @Test
    void labLineNotesArePersonal() throws Exception {
        Session a = register("farhan");
        Session b = register("gita");
        JsonNode projects = body(mvc.perform(get("/api/lab/projects").header("Authorization", a.bearer())).andReturn());
        String templateId = projects.valueStream().filter(p -> p.get("template").asBoolean()).findFirst().orElseThrow()
                .get("id").asText();
        String fileId = body(mvc.perform(get("/api/lab/projects/" + templateId).header("Authorization", a.bearer())).andReturn())
                .get("files").get(0).get("id").asText();

        mvc.perform(put("/api/lab/files/" + fileId + "/lines/3/note").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Maven ka root tag\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/lab/files/" + fileId).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.notes['3']").value("Maven ka root tag"));
        mvc.perform(get("/api/lab/files/" + fileId).header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.notes['3']").doesNotExist());

        // Blank text deletes the note; out-of-range lines are rejected.
        mvc.perform(put("/api/lab/files/" + fileId + "/lines/3/note").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\" \"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/lab/files/" + fileId).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.notes['3']").doesNotExist());
        mvc.perform(put("/api/lab/files/" + fileId + "/lines/99999/note").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"x\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void nextJsTemplateIsOrderedAndExplainedLikeANextProject() throws Exception {
        Session s = register("nisha");
        JsonNode projects = body(mvc.perform(get("/api/lab/projects").header("Authorization", s.bearer())).andReturn());
        JsonNode next = projects.valueStream().filter(p -> p.get("stack").asText().equals("NEXTJS")).findFirst().orElseThrow();
        assertThat(next.get("template").asBoolean()).isTrue();
        assertThat(next.get("stackLabel").asText()).isEqualTo("Next.js");

        JsonNode detail = body(mvc.perform(get("/api/lab/projects/" + next.get("id").asText())
                .header("Authorization", s.bearer())).andReturn());
        List<String> paths = detail.get("files").valueStream().map(f -> f.get("path").asText()).toList();
        assertThat(paths.getFirst()).isEqualTo("package.json");
        assertThat(paths.indexOf("prisma/schema.prisma")).isLessThan(paths.indexOf("prisma/migrations/20260901000000_init/migration.sql"));
        assertThat(paths).containsSubsequence("prisma/schema.prisma", "lib/db.ts", "lib/tasks.ts", "app/actions.ts",
                "app/api/tasks/route.ts", "components/TaskItem.tsx", "components/TaskList.tsx", "app/page.tsx");
        assertThat(paths).noneMatch(p -> p.startsWith("node_modules/") || p.startsWith(".next/") || p.endsWith("package-lock.json"));
        assertThat(detail.get("layers").valueStream().map(l -> l.get("label").asText()))
                .contains("Project setup", "Data access", "API route handlers", "Pages & layouts");

        JsonNode route = detail.get("files").valueStream().filter(f -> f.get("path").asText().equals("app/api/tasks/route.ts"))
                .findFirst().orElseThrow();
        assertThat(route.get("layerLabel").asText()).isEqualTo("API route handlers");
        JsonNode page = detail.get("files").valueStream().filter(f -> f.get("path").asText().equals("app/page.tsx"))
                .findFirst().orElseThrow();
        assertThat(page.get("track").asText()).isEqualTo("FRONTEND");

        String routeId = route.get("id").asText();
        mvc.perform(get("/api/lab/files/" + routeId).header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outline.purpose").value(org.hamcrest.Matchers.containsString("/api/tasks")));
        String source = body(mvc.perform(get("/api/lab/files/" + routeId).header("Authorization", s.bearer())).andReturn())
                .get("lines").toString();
        assertThat(source).contains("export async function GET");
    }

    @Test
    void labProgressRoundTripsThroughAZipOnYourComputer() throws Exception {
        Session s = register("kiran");
        JsonNode projects = body(mvc.perform(get("/api/lab/projects").header("Authorization", s.bearer())).andReturn());
        JsonNode template = projects.valueStream().filter(p -> p.get("template").asBoolean())
                .min(java.util.Comparator.comparingInt(p -> p.get("fileCount").asInt())).orElseThrow();
        String projectId = template.get("id").asText();
        JsonNode detail = body(mvc.perform(get("/api/lab/projects/" + projectId).header("Authorization", s.bearer())).andReturn());
        JsonNode first = detail.get("files").get(0);
        assertThat(first.get("track").asText()).isEqualTo("BACKEND");
        String fileId = first.get("id").asText();
        String path = first.get("path").asText();
        mvc.perform(put("/api/lab/files/" + fileId + "/progress").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"linesCompleted\":5}")).andExpect(status().isOk());

        // Export: typed lines + the full original under _reference/ + a manifest.
        MvcResult export = mvc.perform(get("/api/lab/projects/" + projectId + "/progress/export").header("Authorization", s.bearer()))
                .andExpect(status().isOk()).andReturn();
        assertThat(export.getResponse().getHeader("Content-Disposition")).contains("rebuild-progress.zip");
        java.util.Map<String, String> entries = new java.util.LinkedHashMap<>();
        try (var zin = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(export.getResponse().getContentAsByteArray()))) {
            for (ZipEntry e; (e = zin.getNextEntry()) != null; ) {
                entries.put(e.getName(), new String(zin.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        String root = entries.keySet().iterator().next().split("/")[0] + "/";
        assertThat(entries).containsKey(root + ".rebuild-progress.json").containsKey(root + "_reference/" + path);
        String typed = entries.get(root + path);
        assertThat(typed.lines().count()).isEqualTo(5);

        // Keep typing "locally": 10 correct lines, then a typo.
        java.util.List<String> original = entries.get(root + "_reference/" + path).lines().toList();
        String local = String.join("\n", original.subList(0, 10)) + "\nthis line has a typo\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            add(zos, root + path, local);
            add(zos, root + "_reference/" + path, entries.get(root + "_reference/" + path));
            add(zos, root + ".rebuild-progress.json", entries.get(root + ".rebuild-progress.json"));
        }
        mvc.perform(multipart("/api/lab/projects/" + projectId + "/progress/import")
                        .file(new MockMultipartFile("file", "rebuild.zip", "application/zip", out.toByteArray()))
                        .header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesMatched").value(1))
                .andExpect(jsonPath("$.files[0].before").value(5))
                .andExpect(jsonPath("$.files[0].after").value(10))
                .andExpect(jsonPath("$.files[0].found").value("this line has a typo"));
        mvc.perform(get("/api/lab/files/" + fileId).header("Authorization", s.bearer()))
                .andExpect(jsonPath("$.linesCompleted").value(10))
                .andExpect(jsonPath("$.outline.items").isArray());

        // Someone else's progress is untouched.
        mvc.perform(get("/api/lab/files/" + fileId).header("Authorization", register("lata").bearer()))
                .andExpect(jsonPath("$.linesCompleted").value(0));
    }

    @Test
    void malformedRequestsGetJsonErrorsNotTomcatHtml() throws Exception {
        mvc.perform(get("/api/roadmap;jsessionid=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request_rejected"));
    }

    @Test
    void unknownApiPathsAre404NotTheSpa() throws Exception {
        Session s = register("hari");
        mvc.perform(get("/api/does-not-exist").header("Authorization", s.bearer())).andExpect(status().isNotFound());
    }

    private static byte[] zip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            add(zos, "shop/src/main/java/shop/Product.java", "@Entity\nclass Product {}");
            add(zos, "shop/pom.xml", "<project>\n</project>");
        }
        return out.toByteArray();
    }

    private static void add(ZipOutputStream zos, String name, String content) throws Exception {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
