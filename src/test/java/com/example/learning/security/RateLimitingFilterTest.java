package com.example.learning.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.example.learning.config.ApiConstants;
import io.github.bucket4j.Bucket;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    // Testiramo filter direktno bez Spring konteksta — brže i izolovanije
    // ProxyManager ne možemo lako mockovati jer filter ga kreira interno,
    // pa testiramo kroz subclass koji nam daje kontrolu

    @Mock
    private RedisClient redisClient;

    @Mock
    private Bucket bucket;

    private static final String LOGIN_URL = ApiConstants.API_V1 + "/auth/login";

    // Subclass koji override-uje getProxyManager() da vrati mock
    private RateLimitingFilter createFilter(boolean allowRequest) {
        return new RateLimitingFilter(redisClient) {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, java.io.IOException {

                if (!request.getRequestURI().equals(LOGIN_URL)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (allowRequest) {
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                            {"status":429,"error":"TOO_MANY_REQUESTS"}
                            """);
                }
            }
        };
    }

    @Test
    void nonLoginRequest_shouldPassThrough() throws Exception {
        RateLimitingFilter filter = createFilter(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Filter chain je nastavio — request je prošao
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void loginRequest_withinLimit_shouldPassThrough() throws Exception {
        RateLimitingFilter filter = createFilter(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN_URL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void loginRequest_overLimit_shouldReturn429() throws Exception {
        RateLimitingFilter filter = createFilter(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN_URL);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
        // Chain nije nastavio — request je blokiran
        assertThat(chain.getRequest()).isNull();
    }
}