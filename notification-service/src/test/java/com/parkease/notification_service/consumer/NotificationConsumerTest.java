package com.parkease.notification_service.consumer;

import com.parkease.notification_service.dto.NotificationEvent;
import com.parkease.notification_service.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationConsumer consumer;

    @Test
    @DisplayName("should delegate event to notificationService")
    void consumeSuccess() {
        NotificationEvent event = NotificationEvent.builder()
                .type("BOOKING").recipientId(10L)
                .title("Booking Confirmed").message("Your booking is confirmed")
                .channel("IN_APP").build();

        consumer.consume(event);

        verify(notificationService).createFromEvent(event);
    }

    @Test
    @DisplayName("should not propagate exception from service")
    void consumeError() {
        NotificationEvent event = NotificationEvent.builder()
                .type("BOOKING").recipientId(10L)
                .title("Test").message("Test").channel("IN_APP").build();

        doThrow(new RuntimeException("DB error")).when(notificationService).createFromEvent(event);

        // Should not throw — consumer catches and logs
        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();
    }
}
