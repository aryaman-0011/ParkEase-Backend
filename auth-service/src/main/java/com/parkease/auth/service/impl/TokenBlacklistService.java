package com.parkease.auth.service.impl;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-backed JWT blacklist.
 * When a user logs out, the token is stored in Redis with TTL = remaining validity.
 * JwtAuthenticationFilter checks this before accepting any token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Blacklist a token for the given duration (should be the remaining TTL of the JWT).
     */
    public void blacklist(String token, long remainingMillis) {
        if (remainingMillis <= 0) return;
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(remainingMillis));
        log.info("JWT blacklisted, TTL={}ms", remainingMillis);
    }

    /**
     * Check if a token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
