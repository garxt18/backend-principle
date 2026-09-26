package com.backendprinciple.playground.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Coarse per-IP limits applied before security runs: a strict limit on the auth endpoints
 * (slows down password guessing) and a generous one on the rest of the API.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter authLimiter;
    private final RateLimiter apiLimiter;

    public RateLimitFilter(@Value("${app.rate-limit.auth-per-minute}") int authPerMinute,
                           @Value("${app.rate-limit.api-per-minute}") int apiPerMinute) {
        this.authLimiter = new RateLimiter(authPerMinute, Duration.ofMinutes(1));
        this.apiLimiter = new RateLimiter(apiPerMinute, Duration.ofMinutes(1));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // With server.forward-headers-strategy=native, Tomcat rewrites getRemoteAddr() from X-Forwarded-For
        // only when the direct peer is a trusted (private-network) proxy, so clients cannot spoof their IP.
        String ip = request.getRemoteAddr();
        boolean isAuth = request.getRequestURI().startsWith("/api/auth/")
                && !request.getRequestURI().equals("/api/auth/refresh");
        long wait = isAuth ? authLimiter.tryAcquire("auth:" + ip) : apiLimiter.tryAcquire("api:" + ip);
        if (wait > 0) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(wait));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"https://backend-playground.dev/errors/rate_limited","title":"Too Many Requests",\
                    "status":429,"code":"rate_limited","detail":"Too many requests - try again in %d s"}"""
                    .formatted(wait));
            return;
        }
        chain.doFilter(request, response);
    }
}
