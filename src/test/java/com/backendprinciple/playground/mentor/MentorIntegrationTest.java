package com.backendprinciple.playground.mentor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backendprinciple.playground.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the real Spring AI ChatClient code path with a stub ChatModel (no API key, no network),
 * plus the daily quota and the shared explanation cache.
 */
@Import(MentorIntegrationTest.StubModel.class)
@TestPropertySource(properties = {"app.ai.daily-quota=3", "app.ai.per-minute=100"})
@EnabledIf("com.backendprinciple.playground.AbstractIntegrationTest#databaseAvailable")
class MentorIntegrationTest extends AbstractIntegrationTest {

    static final AtomicInteger CALLS = new AtomicInteger();
    static final AtomicReference<Prompt> LAST_PROMPT = new AtomicReference<>();

    @TestConfiguration
    static class StubModel {
        @Bean
        ChatModel stubChatModel() {
            return prompt -> {
                CALLS.incrementAndGet();
                LAST_PROMPT.set(prompt);
                String user = prompt.getUserMessage().getText();
                String answer = user.contains("quiz")
                        ? """
                          {"topic":"JPA","questions":[{"question":"What does @Id mark?","options":["PK","FK","Index","Column"],
                          "correctIndex":0,"explanation":"The primary key."}]}"""
                        : "**Dependency injection** means Spring passes collaborators into your constructor.";
                return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
            };
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void emptyCache() {
        jdbc.sql("DELETE FROM ai_explanations").update(); // the cache is shared, so start each test clean
    }

    private String login() throws Exception {
        var res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ai-%s@example.com","password":"password123","displayName":"Neha","preferredLanguage":"HINDI"}"""
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn();
        return "Bearer " + json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void askUsesUserLanguageAndEnforcesDailyQuota() throws Exception {
        String auth = login();
        mvc.perform(get("/api/mentor/status").header("Authorization", auth))
                .andExpect(jsonPath("$.enabled").value(true));

        mvc.perform(post("/api/mentor/ask").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"DI kya hai?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("Dependency injection")))
                .andExpect(jsonPath("$.usedToday").value(1));
        assertThat(LAST_PROMPT.get().getSystemMessage().getText()).contains("Hinglish");
        assertThat(LAST_PROMPT.get().getUserMessage().getText()).contains("DI kya hai?");

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/mentor/ask").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"again\"}")).andExpect(status().isOk());
        }
        mvc.perform(post("/api/mentor/ask").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"one too many\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void lineExplanationsAreCachedAcrossUsers() throws Exception {
        String first = login();
        JsonNode projects = json.readTree(mvc.perform(get("/api/lab/projects").header("Authorization", first))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        String templateId = projects.valueStream().filter(p -> p.get("name").asText().startsWith("Task Manager"))
                .findFirst().orElseThrow().get("id").asText();
        JsonNode detail = json.readTree(mvc.perform(get("/api/lab/projects/" + templateId).header("Authorization", first))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        String fileId = detail.get("files").get(6).get("id").asText();

        int before = CALLS.get();
        mvc.perform(post("/api/mentor/explain/" + fileId + "/3").header("Authorization", first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(false));
        mvc.perform(post("/api/mentor/explain/" + fileId + "/3").header("Authorization", login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true))
                .andExpect(jsonPath("$.usedToday").value(0)); // cache hits are free
        assertThat(CALLS.get() - before).isEqualTo(1);
    }

    @Test
    void quizIsParsedIntoJavaRecords() throws Exception {
        String auth = login();
        long topicId = json.readTree(mvc.perform(get("/api/roadmap")).andReturn().getResponse().getContentAsString())
                .get(3).get("topics").get(4).get("id").asLong();
        mvc.perform(post("/api/mentor/quiz/" + topicId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options.length()").value(4))
                .andExpect(jsonPath("$.questions[0].correctIndex").value(0));
    }
}
