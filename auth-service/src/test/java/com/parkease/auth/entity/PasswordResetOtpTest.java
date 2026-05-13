package com.parkease.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PasswordResetOtpTest {

    @Test
    void onCreateSetsCreatedAtAndMarksOtpUnused() {
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email("user@test.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(true)
                .build();

        otp.onCreate();

        assertThat(otp.getCreatedAt()).isNotNull();
        assertThat(otp.isUsed()).isFalse();
    }

    @Test
    void isExpiredReflectsExpirationTime() {
        PasswordResetOtp expired = PasswordResetOtp.builder()
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        PasswordResetOtp active = PasswordResetOtp.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(1))
                .build();

        assertThat(expired.isExpired()).isTrue();
        assertThat(active.isExpired()).isFalse();
    }
}
