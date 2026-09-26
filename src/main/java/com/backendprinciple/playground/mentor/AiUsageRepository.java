package com.backendprinciple.playground.mentor;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain JDBC (JdbcClient) instead of JPA: an atomic "insert or increment" is one SQL statement in
 * PostgreSQL and needs no entity. Using the right tool per query is normal in real projects.
 */
@Repository
public class AiUsageRepository {

    private final JdbcClient jdbc;

    public AiUsageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Atomically adds one request and returns the new count for that day (safe under concurrency). */
    public int increment(UUID userId, LocalDate day) {
        return jdbc.sql("""
                        INSERT INTO ai_usage (user_id, usage_date, requests) VALUES (:user, :day, 1)
                        ON CONFLICT (user_id, usage_date) DO UPDATE SET requests = ai_usage.requests + 1
                        RETURNING requests""")
                .param("user", userId)
                .param("day", day)
                .query(Integer.class)
                .single();
    }

    public void decrement(UUID userId, LocalDate day) {
        jdbc.sql("UPDATE ai_usage SET requests = GREATEST(requests - 1, 0) WHERE user_id = :user AND usage_date = :day")
                .param("user", userId)
                .param("day", day)
                .update();
    }

    public int usedOn(UUID userId, LocalDate day) {
        return jdbc.sql("SELECT requests FROM ai_usage WHERE user_id = :user AND usage_date = :day")
                .param("user", userId)
                .param("day", day)
                .query(Integer.class)
                .optional()
                .orElse(0);
    }
}
