package com.routineflow.unit.security;

import com.routineflow.infrastructure.security.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Fresh mock request for POST /auth/login from the given IP. */
    private MockHttpServletRequest loginRequest(String remoteIp) {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr(remoteIp);
        return request;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ten requests from the same IP all pass")
    void tenRequestsPass() throws Exception {
        for (int i = 0; i < 10; i++) {
            var response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("10.0.0.1"), response, new MockFilterChain());
            assertThat(response.getStatus())
                    .as("request %d should not be rate-limited", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("eleventh request from the same IP receives HTTP 429")
    void eleventhRequestReturns429() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("10.0.0.2"), new MockHttpServletResponse(), new MockFilterChain());
        }

        var response = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.2"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    @DisplayName("rate limit does NOT apply to GET /analytics/streaks (shouldNotFilter)")
    void otherEndpointIsNotRateLimited() throws Exception {
        var request = new MockHttpServletRequest("GET", "/analytics/streaks");
        request.setServletPath("/analytics/streaks");
        request.setRemoteAddr("10.0.0.3");

        // Simulate 20 requests — none should be rate-limited because the filter skips this path
        for (int i = 0; i < 20; i++) {
            var req = new MockHttpServletRequest("GET", "/analytics/streaks");
            req.setServletPath("/analytics/streaks");
            req.setRemoteAddr("10.0.0.3");

            var response = new MockHttpServletResponse();
            filter.doFilter(req, response, new MockFilterChain());

            assertThat(response.getStatus())
                    .as("GET request %d should not be rate-limited", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("different IPs have independent buckets — 10 requests each pass")
    void differentIpsHaveIndependentBuckets() throws Exception {
        String ipA = "192.168.1.1";
        String ipB = "192.168.1.2";

        // Exhaust the bucket for ipA
        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest(ipA), new MockHttpServletResponse(), new MockFilterChain());
        }

        // ipA is now rate-limited
        var responseA = new MockHttpServletResponse();
        filter.doFilter(loginRequest(ipA), responseA, new MockFilterChain());
        assertThat(responseA.getStatus()).isEqualTo(429);

        // ipB should still be unaffected — its bucket is independent
        var responseB = new MockHttpServletResponse();
        filter.doFilter(loginRequest(ipB), responseB, new MockFilterChain());
        assertThat(responseB.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("getClientIp prefers X-Forwarded-For over remoteAddr")
    void getClientIpPrefersXForwardedFor() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.99");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.99");

        String ip = filter.getClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.1");
    }
}
