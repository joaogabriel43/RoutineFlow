package com.routineflow.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits POST /auth/login to 10 requests per IP per minute.
 *
 * <p>Each unique IP gets its own {@link Bucket} stored in a
 * {@link ConcurrentHashMap}. Requests beyond the limit receive HTTP 429.</p>
 *
 * <p>Respects {@code X-Forwarded-For} so that reverse-proxy deployments
 * (Railway, Render, Vercel) rate-limit the real client IP, not the proxy.</p>
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int    REQUESTS_PER_WINDOW = 10;
    private static final Duration WINDOW            = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // ── Filter gate ───────────────────────────────────────────────────────────

    /**
     * Skip the filter for any request that is NOT {@code POST /auth/login}.
     * Note: {@code getServletPath()} returns the path without the context-path prefix
     * (i.e., without {@code /api}), which is what we want to match against.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/auth/login".equals(request.getServletPath());
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Bucket bucket   = buckets.computeIfAbsent(clientIp, ip -> createBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Too many requests. Please try again in a minute.\"}");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REQUESTS_PER_WINDOW)
                .refillIntervally(REQUESTS_PER_WINDOW, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extracts the real client IP, respecting {@code X-Forwarded-For}.
     * When behind a reverse proxy the header may contain a comma-separated list
     * of IPs — only the first (original client) is used.
     * Package-visible for unit testing.
     */
    public String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
