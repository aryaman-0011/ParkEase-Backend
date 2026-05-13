package com.parkease.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    void blacklistStoresTokenForRemainingLifetime() {
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.blacklist("jwt-token", 60_000L);

        verify(valueOperations).set("jwt:blacklist:jwt-token", "1", Duration.ofMillis(60_000L));
    }

    @Test
    void blacklistIgnoresExpiredTokens() {
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

        service.blacklist("jwt-token", 0L);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void isBlacklistedChecksTokenKey() {
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);
        when(redisTemplate.hasKey("jwt:blacklist:jwt-token")).thenReturn(true);

        assertThat(service.isBlacklisted("jwt-token")).isTrue();
    }
}
