package com.example.learning.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    private static final String TOKEN = "some.jwt.token";
    private static final String KEY = "blacklist:" + TOKEN;

    @Test
    void blacklistToken_shouldStoreTokenInRedisWithTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBlacklistService.blacklistToken(TOKEN, 60000L);

        verify(valueOperations).set(KEY, "revoked", 60000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void isBlacklisted_whenTokenExists_shouldReturnTrue() {
        when(redisTemplate.hasKey(KEY)).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted(TOKEN)).isTrue();
    }

    @Test
    void isBlacklisted_whenTokenNotExists_shouldReturnFalse() {
        when(redisTemplate.hasKey(KEY)).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted(TOKEN)).isFalse();
    }

    @Test
    void isBlacklisted_whenRedisReturnsNull_shouldReturnFalse() {
        when(redisTemplate.hasKey(KEY)).thenReturn(null);

        assertThat(tokenBlacklistService.isBlacklisted(TOKEN)).isFalse();
    }
}