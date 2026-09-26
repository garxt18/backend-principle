package com.backendprinciple.playground.auth;

import com.backendprinciple.playground.common.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableScheduling
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties props, Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    /** Result of issuing or rotating: the raw token (sent to the browser once) and the owner. */
    public record Issued(String rawToken, UUID userId) {
    }

    @Transactional
    public Issued issueNewFamily(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /**
     * Exchanges a valid refresh token for a new one. Presenting an already-used token means it was
     * probably stolen, so the whole family is revoked and the user must log in again.
     * <p>noRollbackFor: the family revocation must be committed even though we then reject the request.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Issued rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("Session expired - please log in again"));
        Instant now = clock.instant();
        if (current.isRevoked()) {
            int revoked = repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected for user {} - revoked {} tokens", current.getUserId(), revoked);
            throw ApiException.unauthorized("Session expired - please log in again");
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized("Session expired - please log in again");
        }
        current.revoke(now);
        return issue(current.getUserId(), current.getFamilyId());
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    /** Housekeeping: drop tokens that expired more than a day ago. Runs hourly. */
    @Scheduled(cron = "0 17 * * * *")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpiredBefore(clock.instant().minus(1, ChronoUnit.DAYS));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }

    private Issued issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        repository.save(new RefreshToken(userId, hash(raw), familyId, now, now.plus(props.refreshTokenTtl())));
        return new Issued(raw, userId);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
