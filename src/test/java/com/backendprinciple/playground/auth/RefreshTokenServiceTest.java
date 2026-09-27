package com.backendprinciple.playground.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backendprinciple.playground.common.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private static final Instant T0 = Instant.parse("2026-09-27T10:00:00Z");
    private final RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
    private final JwtProperties props = new JwtProperties("x".repeat(40), "test", Duration.ofMinutes(15),
            Duration.ofDays(14), false, Duration.ofSeconds(30));

    private RefreshTokenService at(Instant now) {
        return new RefreshTokenService(repo, props, Clock.fixed(now, ZoneOffset.UTC));
    }

    private RefreshToken rotatedToken(Instant rotatedAt) {
        RefreshToken t = new RefreshToken(UUID.randomUUID(), RefreshTokenService.hash("old"), UUID.randomUUID(),
                T0.minusSeconds(3600), T0.plus(Duration.ofDays(13)));
        t.revoke(rotatedAt);
        when(repo.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void aSecondTabRefreshingRightAfterRotationGetsItsOwnToken() {
        RefreshToken old = rotatedToken(T0);
        when(repo.existsByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq(old.getFamilyId()), any())).thenReturn(true);

        RefreshTokenService.Issued issued = at(T0.plusSeconds(5)).rotate("old");

        assertThat(issued.userId()).isEqualTo(old.getUserId());
        verify(repo, never()).revokeFamily(any(), any());
    }

    @Test
    void replayingARotatedTokenLaterIsTreatedAsTheftAndKillsTheSession() {
        RefreshToken old = rotatedToken(T0);
        when(repo.existsByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq(old.getFamilyId()), any())).thenReturn(true);

        assertThatThrownBy(() -> at(T0.plusSeconds(120)).rotate("old")).isInstanceOf(ApiException.class);
        verify(repo).revokeFamily(eq(old.getFamilyId()), any());
    }

    @Test
    void afterLogoutTheGraceWindowDoesNotApply() {
        RefreshToken old = rotatedToken(T0);
        when(repo.existsByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq(old.getFamilyId()), any())).thenReturn(false);

        assertThatThrownBy(() -> at(T0.plusSeconds(5)).rotate("old")).isInstanceOf(ApiException.class);
    }
}
