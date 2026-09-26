package com.backendprinciple.playground.auth;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.user.PreferredLanguage;
import com.backendprinciple.playground.user.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login returns a short-lived access token in the JSON body (kept in browser memory) and a long-lived
 * refresh token in an HttpOnly cookie that JavaScript cannot read - so an XSS bug cannot steal it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "pg_refresh";

    private final AuthService authService;
    private final JwtProperties props;

    public AuthController(AuthService authService, JwtProperties props) {
        this.authService = authService;
        this.props = props;
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 128, message = "must be 8-128 characters") String password,
            @NotBlank @Size(min = 2, max = 80) String displayName,
            PreferredLanguage preferredLanguage) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        var session = authService.register(req.email(), req.password(), req.displayName(), req.preferredLanguage());
        return respond(HttpStatus.CREATED, session);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return respond(HttpStatus.OK, authService.login(req.email(), req.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.unauthorized("Not logged in");
        }
        try {
            return respond(HttpStatus.OK, authService.refresh(token));
        } catch (ApiException e) {
            // Clear a dead cookie so the browser stops sending it.
            return ResponseEntity.status(e.status()).header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthService.Session s) {
        ResponseCookie refresh = cookie(s.refreshToken(), props.refreshTokenTtl().toSeconds());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new AuthResponse(s.accessToken(), "Bearer", s.expiresIn(), s.user()));
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.secureCookie())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
