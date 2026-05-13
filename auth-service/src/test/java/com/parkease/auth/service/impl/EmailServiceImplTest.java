package com.parkease.auth.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
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

    private void failWhenSettingRecipient() throws MessagingException {
        doThrow(new MessagingException("bad content"))
                .when(mimeMessage).setContent(any(Multipart.class));
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

        @Test
        @DisplayName("should wrap MessagingException on OTP email")
        void sendMessagingException() throws MessagingException {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            failWhenSettingRecipient();

            assertThatThrownBy(() -> emailService.sendOtpEmail("user@test.com", "123456"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to send OTP email");
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

        @Test
        @DisplayName("should suppress MessagingException for login notification")
        void sendMessagingException() throws MessagingException {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            failWhenSettingRecipient();

            assertThatCode(() -> emailService.sendLoginNotificationEmail(
                    "user@test.com", "John", "Email & Password"))
                    .doesNotThrowAnyException();

            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendWelcomeEmail")
    class SendWelcomeEmail {
        @Test
        @DisplayName("should send welcome email")
        void sendWelcome() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendWelcomeEmail("user@test.com", "John");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should handle blank name in welcome email")
        void sendWelcomeWithBlankName() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatCode(() -> emailService.sendWelcomeEmail("user@test.com", "   "))
                    .doesNotThrowAnyException();

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should suppress MessagingException for welcome email")
        void sendWelcomeMessagingException() throws MessagingException {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            failWhenSettingRecipient();

            assertThatCode(() -> emailService.sendWelcomeEmail("user@test.com", "John"))
                    .doesNotThrowAnyException();

            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendReceiptEmail")
    class SendReceiptEmail {
        @Test
        @DisplayName("should send receipt email")
        void sendReceipt() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendReceiptEmail("user@test.com", "John", java.util.Map.ofEntries(
                    java.util.Map.entry("receiptNumber", "PE-123"),
                    java.util.Map.entry("amount", "250"),
                    java.util.Map.entry("status", "SUCCESS"),
                    java.util.Map.entry("paymentMethod", "UPI"),
                    java.util.Map.entry("transactionId", "txn-1"),
                    java.util.Map.entry("date", "2026-05-13"),
                    java.util.Map.entry("lotName", "Central Lot"),
                    java.util.Map.entry("spotNumber", "A-01"),
                    java.util.Map.entry("vehiclePlate", "KA01AB1234"),
                    java.util.Map.entry("duration", "2h"),
                    java.util.Map.entry("description", "Parking fee")));

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should render receipt defaults and alternate status")
        void sendReceiptWithDefaults() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatCode(() -> emailService.sendReceiptEmail(
                    "user@test.com", null, java.util.Map.of("status", "FAILED")))
                    .doesNotThrowAnyException();

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should render refunded and pending receipts")
        void sendReceiptWithOtherStatuses() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThatCode(() -> {
                emailService.sendReceiptEmail("user@test.com", "John",
                        java.util.Map.of("status", "REFUNDED"));
                emailService.sendReceiptEmail("user@test.com", "John",
                        java.util.Map.of("status", "PENDING"));
            }).doesNotThrowAnyException();

            verify(mailSender, times(2)).send(mimeMessage);
        }

        @Test
        @DisplayName("should wrap MessagingException for receipt email")
        void sendReceiptMessagingException() throws MessagingException {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            failWhenSettingRecipient();

            assertThatThrownBy(() -> emailService.sendReceiptEmail(
                    "user@test.com", "John", java.util.Map.of("status", "SUCCESS")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to send receipt email");
        }
    }
}
