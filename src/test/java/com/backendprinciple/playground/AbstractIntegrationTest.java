package com.backendprinciple.playground;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests run against a real PostgreSQL:
 * <ul>
 *   <li>by default a throw-away container started by Testcontainers (needs Docker - this is what CI uses);</li>
 *   <li>or an existing database when TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD are set.</li>
 * </ul>
 * With neither available the tests are skipped instead of failing. Note: JUnit's {@code @EnabledIf} is not
 * inherited, so every concrete test class repeats it.
 */
@SpringBootTest(properties = {
        "app.rate-limit.auth-per-minute=1000",
        "app.jwt.secret=integration-test-secret-integration-test-secret"
})
@AutoConfigureMockMvc
@EnabledIf("com.backendprinciple.playground.AbstractIntegrationTest#databaseAvailable")
public abstract class AbstractIntegrationTest {

    private static PostgreSQLContainer postgres;

    static boolean databaseAvailable() {
        return System.getenv("TEST_DB_URL") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DB_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "playground"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "playground"));
            return;
        }
        if (postgres == null) {
            postgres = new PostgreSQLContainer("postgres:16-alpine");
            postgres.start(); // shared by all test classes; Testcontainers' Ryuk removes it after the JVM exits
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
