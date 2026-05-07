package com.parkease.auth.service.impl;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-backed OTP storage.
 * Replaces MySQL PasswordResetOtp table with auto-expiring Redis keys.
 *
 * Key patterns:
 *   otp:{email}:code   → the OTP value (TTL = expiration minutes)
 *   otp:{email}:ts     → timestamp of last OTP request (TTL = 60s, for rate limiting)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOtpService {

    private final StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final String CODE_SUFFIX = ":code";
    private static final String RATE_LIMIT_SUFFIX = ":ts";

    /**
     * Store an OTP for the given email with TTL-based expiration.
     */
    public void storeOtp(String email, String otp, int expirationMinutes) {
        String key = OTP_PREFIX + email.toLowerCase() + CODE_SUFFIX;
        String rateLimitKey = OTP_PREFIX + email.toLowerCase() + RATE_LIMIT_SUFFIX;

        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(expirationMinutes));
        redisTemplate.opsForValue().set(rateLimitKey, String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(60));
        log.info("OTP stored in Redis for email={}, TTL={}min", email, expirationMinutes);
    }

    /**
     * Validate an OTP. Returns true if valid and not yet used.
     */
    public boolean validateOtp(String email, String otp) {
        String key = OTP_PREFIX + email.toLowerCase() + CODE_SUFFIX;
        String storedOtp = redisTemplate.opsForValue().get(key);
        return storedOtp != null && storedOtp.equals(otp);
    }

    /**
     * Consume (invalidate) an OTP after successful password reset.
     */
    public void invalidateOtp(String email) {
        String key = OTP_PREFIX + email.toLowerCase() + CODE_SUFFIX;
        redisTemplate.delete(key);
        log.info("OTP invalidated in Redis for email={}", email);
    }

    /**
     * Check if an OTP was recently requested (within 60 seconds).
     */
    public boolean isRateLimited(String email) {
        String rateLimitKey = OTP_PREFIX + email.toLowerCase() + RATE_LIMIT_SUFFIX;
        return Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey));
    }

    /**
     * Delete all OTPs for an email (used during account deletion).
     */
    public void deleteAllForEmail(String email) {
        String codeKey = OTP_PREFIX + email.toLowerCase() + CODE_SUFFIX;
        String tsKey = OTP_PREFIX + email.toLowerCase() + RATE_LIMIT_SUFFIX;
        redisTemplate.delete(codeKey);
        redisTemplate.delete(tsKey);
    }
}
