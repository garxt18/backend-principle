package com.backendprinciple.playground.mentor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Shared, database-backed cache of AI answers keyed by a SHA-256 of the prompt. */
@Repository
public class AiExplanationCache {

    private final JdbcClient jdbc;
    private final Clock clock;

    public AiExplanationCache(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public Optional<String> get(String key) {
        return jdbc.sql("SELECT explanation FROM ai_explanations WHERE cache_key = :key")
                .param("key", key)
                .query(String.class)
                .optional();
    }

    public void put(String key, String explanation) {
        jdbc.sql("""
                        INSERT INTO ai_explanations (cache_key, explanation, created_at) VALUES (:key, :text, :now)
                        ON CONFLICT (cache_key) DO NOTHING""")
                .param("key", key)
                .param("text", explanation)
                .param("now", java.sql.Timestamp.from(clock.instant()))
                .update();
    }

    public static String keyOf(String... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String p : parts) {
                md.update(p.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
