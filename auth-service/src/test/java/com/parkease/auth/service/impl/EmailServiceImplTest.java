package com.parkease.auth.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
    }

    @Nested
    @DisplayName("sendOtpEmail")
    class SendOtpEmail {
        @Test
        @DisplayName("should send OTP email successfully")
        void sendSuccess() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendOtpEmail("user@test.com", "123456");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should propagate RuntimeException on mail send failure")
        void sendFailure() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .when(mailSender).send(any(MimeMessage.class));

            // MailSendException extends RuntimeException - it propagates since
            // the catch block only handles MessagingException
            assertThatThrownBy(() -> emailService.sendOtpEmail("user@test.com", "123456"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("sendLoginNotificationEmail")
    class SendLoginNotification {
        @Test
        @DisplayName("should send login notification email")
        void sendSuccess() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendLoginNotificationEmail("user@test.com", "John", "Google");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should handle null fullName gracefully")
        void sendWithNullName() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatCode(() -> emailService.sendLoginNotificationEmail(
                    "user@test.com", null, "Email & Password"))
                    .doesNotThrowAnyException();

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should handle null loginMethod gracefully")
        void sendWithNullMethod() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatCode(() -> emailService.sendLoginNotificationEmail(
                    "user@test.com", "John", null))
                    .doesNotThrowAnyException();

            verify(mailSender).send(mimeMessage);
        }
    }
}
