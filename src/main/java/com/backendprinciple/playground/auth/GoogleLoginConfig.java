package com.backendprinciple.playground.auth;

import com.backendprinciple.playground.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * "Continue with Google" - the OAuth 2.0 authorization-code flow with OpenID Connect (Spring Boot lecture 39):
 * <pre>
 * Browser -> /oauth2/authorization/google -> Google sign-in -> /login/oauth2/code/google?code=...
 *         -> Spring exchanges the code, verifies Google's signed ID token -> onGoogleLogin()
 *         -> our own refresh cookie is set -> redirect to /dashboard -> the React app refreshes as usual
 * </pre>
 * Only these redirect URLs use a (short-lived) HTTP session, to remember the OAuth "state"; the API stays
 * stateless. Active only when app.google.client-id and client-secret are set.
 */
@Configuration
@ConditionalOnExpression("!'${app.google.client-id:}'.isBlank() and !'${app.google.client-secret:}'.isBlank()")
public class GoogleLoginConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginConfig.class);

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(GoogleLoginProperties google) {
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(google.clientId().strip())
                .clientSecret(google.clientSecret().strip())
                .redirectUri(google.redirectUri())
                .scope("openid", "email", "profile")
                .build());
    }

    /** Ordered before the main API chain, and only for the OAuth redirect URLs. */
    @Bean
    @Order(1)
    SecurityFilterChain googleLoginChain(HttpSecurity http, AuthService authService, JwtProperties jwt) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // Nothing to remember after the redirect: our own tokens take over.
                .securityContext(c -> c.securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .oauth2Login(o -> o
                        .successHandler(onGoogleLogin(authService, jwt))
                        .failureHandler((request, response, ex) -> {
                            log.warn("Google sign-in failed: {}", ex.getMessage());
                            finish(request, response, "/login?error=google");
                        }));
        return http.build();
    }

    static AuthenticationSuccessHandler onGoogleLogin(AuthService authService, JwtProperties jwt) {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            OidcUser google = (OidcUser) authentication.getPrincipal();
            try {
                AuthService.Session session = authService.loginWithGoogle(google.getSubject(), google.getEmail(),
                        Boolean.TRUE.equals(google.getEmailVerified()), google.getFullName());
                response.addHeader(HttpHeaders.SET_COOKIE, AuthController.refreshCookie(jwt, session.refreshToken(),
                        jwt.refreshTokenTtl().toSeconds()).toString());
                finish(request, response, "/dashboard");
            } catch (ApiException e) {
                finish(request, response, "/login?error=" + (e.getMessage().contains("disabled") ? "disabled" : "unverified"));
            }
        };
    }

    private static void finish(HttpServletRequest request, HttpServletResponse response, String target) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.sendRedirect(target);
    }
}
