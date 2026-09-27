package com.backendprinciple.playground.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Type-safe binding of the {@code app.jwt.*} settings. Startup fails fast if they are invalid. */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters for HS256") String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        boolean secureCookie,
        /* two tabs refreshing with the same cookie within this window is a race, not theft */
        @NotNull Duration refreshReuseGrace) {
}
