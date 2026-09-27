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
    void refreshTokensRotateAndReuseKillsTheFamily() throws Exception {
        Session s = register("ravi");
        MvcResult first = mvc.perform(post("/api/auth/refresh").cookie(s.refresh())).andExpect(status().isOk()).andReturn();
        Cookie rotated = first.getResponse().getCookie("pg_refresh");
        assertThat(rotated.getValue()).isNotEqualTo(s.refresh().getValue());

        // An attacker replays the old token: rejected, and the legitimate new token dies with it.
        mvc.perform(post("/api/auth/refresh").cookie(s.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(rotated)).andExpect(status().isUnauthorized());
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
    void dsaSheetTracksSolvedProblemsAndFeedsPlanly() throws Exception {
        Session s = register("tara");
        JsonNode sheet = body(mvc.perform(get("/api/dsa/sheet").header("Authorization", s.bearer()))
                .andExpect(status().isOk()).andReturn());
        assertThat(sheet.get("stats").get("total").asInt()).isGreaterThan(100);
        long twoSum = sheet.get("topics").get(0).get("problems").get(0).get("id").asLong();

        mvc.perform(put("/api/dsa/problems/" + twoSum).header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solved\":true,\"revision\":true,\"notes\":\"HashMap of seen values\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.solvedAt").exists());
        mvc.perform(get("/api/dsa/sheet").header("Authorization", s.bearer()))
                .andExpect(jsonPath("$.stats.solved").value(1))
                .andExpect(jsonPath("$.stats.revision").value(1))
                .andExpect(jsonPath("$.stats.easy.solved").value(1))
                .andExpect(jsonPath("$.topics[0].problems[0].notes").value("HashMap of seen values"));

        // Planly counts problems solved during the current week against the weekly DSA target.
        mvc.perform(post("/api/plans").header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hoursPerWeek\":15,\"dsaPerWeek\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dsaPerWeek").value(10))
                .andExpect(jsonPath("$.weeks[0].dsaTarget").value(10))
                .andExpect(jsonPath("$.weeks[0].dsaSolved").value(1));

        // Another learner's sheet is untouched.
        mvc.perform(get("/api/dsa/sheet").header("Authorization", register("uma").bearer()))
                .andExpect(jsonPath("$.stats.solved").value(0));
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
