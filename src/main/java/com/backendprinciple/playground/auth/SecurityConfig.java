package com.backendprinciple.playground.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Request security flow (roadmap Level 6):
 * <pre>
 * Client -> [BearerTokenAuthenticationFilter: validates JWT signature + expiry]
 *        -> [AuthorizationFilter: checks the URL rules below / @PreAuthorize]
 *        -> Controller
 * </pre>
 * We use Spring Security's built-in OAuth2 Resource Server JWT support instead of a hand-written
 * filter: less code to get wrong, and the same setup works later with an external identity provider.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${app.cors.allowed-origins:}") String corsOrigins) throws Exception {
        http
                // Stateless API authenticated by an Authorization header -> CSRF does not apply.
                // The only cookie (refresh token) is SameSite=Strict and scoped to /api/auth.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource(corsOrigins)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/roadmap/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is the React app and its static files (public; data comes from /api).
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; "
                                        + "base-uri 'self'; form-action 'self'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    /** Turns the JWT's "role" claim into a Spring authority like ROLE_ADMIN. */
    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))));
        return converter;
    }

    private CorsConfigurationSource corsSource(String origins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!origins.isBlank()) {
            CorsConfiguration cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
            cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
            cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
            cfg.setAllowCredentials(true);
            source.registerCorsConfiguration("/api/**", cfg);
        }
        return source;
    }

    @Bean
    SecretKey jwtSigningKey(JwtProperties props) {
        return new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        return decoder;
    }

    /**
     * Spring Security's firewall rejects malformed requests (control characters in headers, "//" in paths...).
     * By default it calls sendError(400), which re-runs the same rejected request through the error page and ends
     * in Tomcat's bare HTML page. Answer with our normal JSON error instead.
     */
    @Bean
    RequestRejectedHandler requestRejectedHandler() {
        return (request, response, ex) -> {
            response.setStatus(400);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"https://backend-playground.dev/errors/bad_request","title":"Bad Request","status":400,\
                    "code":"request_rejected","detail":"The browser sent a malformed request (bad header or URL). \
                    Try clearing this site's cookies."}""");
        };
    }

    /** BCrypt by default, with a "{bcrypt}" prefix so the algorithm can be upgraded later (e.g. Argon2). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
