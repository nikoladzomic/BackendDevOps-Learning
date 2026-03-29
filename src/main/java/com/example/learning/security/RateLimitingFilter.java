package com.example.learning.security;

import com.example.learning.config.ApiConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisClient redisClient;
    private volatile ProxyManager<byte[]> proxyManager;

    public RateLimitingFilter(RedisClient redisClient) {
        this.redisClient = redisClient;
        // NE konektujemo se ovde
    }

    private ProxyManager<byte[]> getProxyManager() {
        if (proxyManager == null) {
            synchronized (this) {
                if (proxyManager == null) {
                    StatefulRedisConnection<byte[], byte[]> connection =
                            redisClient.connect(ByteArrayCodec.INSTANCE);
                    this.proxyManager = LettuceBasedProxyManager
                            .builderFor(connection)
                            .build();
                }
            }
        }
        return proxyManager;
    }

    private BucketConfiguration createBucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        10,
                        Refill.greedy(10, Duration.ofMinutes(1))
                ))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI().equals(ApiConstants.API_V1 + "/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        byte[] key = ("rate_limit:" + ip).getBytes();

        Supplier<BucketConfiguration> configSupplier = this::createBucketConfiguration;

        var bucket = getProxyManager().builder().build(key, configSupplier);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {
                        "status": 429,
                        "error": "TOO_MANY_REQUESTS",
                        "message": "Too many login attempts. Try again in 1 minute."
                    }
                    """);
        }
    }
}