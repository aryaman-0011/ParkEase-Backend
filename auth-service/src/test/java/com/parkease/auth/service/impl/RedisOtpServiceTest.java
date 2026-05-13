package com.parkease.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
class RedisOtpServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    void storeOtpWritesCodeAndRateLimitKeysWithTtl() {
        RedisOtpService service = new RedisOtpService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.storeOtp("USER@Test.com", "123456", 5);

        verify(valueOperations).set("otp:user@test.com:code", "123456", Duration.ofMinutes(5));
        verify(valueOperations).set(eq("otp:user@test.com:ts"), org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void validateOtpReturnsTrueOnlyWhenStoredCodeMatches() {
        RedisOtpService service = new RedisOtpService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:user@test.com:code")).thenReturn("123456");

        assertThat(service.validateOtp("USER@Test.com", "123456")).isTrue();
        assertThat(service.validateOtp("USER@Test.com", "000000")).isFalse();
    }

    @Test
    void validateOtpReturnsFalseWhenCodeExpired() {
        RedisOtpService service = new RedisOtpService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:user@test.com:code")).thenReturn(null);

        assertThat(service.validateOtp("user@test.com", "123456")).isFalse();
    }

    @Test
    void invalidateOtpDeletesCodeKey() {
        RedisOtpService service = new RedisOtpService(redisTemplate);

        service.invalidateOtp("USER@Test.com");

        verify(redisTemplate).delete("otp:user@test.com:code");
    }

    @Test
    void isRateLimitedChecksTimestampKey() {
        RedisOtpService service = new RedisOtpService(redisTemplate);
        when(redisTemplate.hasKey("otp:user@test.com:ts")).thenReturn(true);

        assertThat(service.isRateLimited("USER@Test.com")).isTrue();
    }

    @Test
    void deleteAllForEmailRemovesCodeAndTimestampKeys() {
        RedisOtpService service = new RedisOtpService(redisTemplate);

        service.deleteAllForEmail("USER@Test.com");

        verify(redisTemplate).delete("otp:user@test.com:code");
        verify(redisTemplate).delete("otp:user@test.com:ts");
    }
}
