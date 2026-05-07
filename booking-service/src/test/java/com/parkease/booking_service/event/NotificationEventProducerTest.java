package com.parkease.booking_service.event;

import com.parkease.booking_service.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private NotificationEventProducer producer;

    @Test
    @DisplayName("should publish event to correct exchange and routing key")
    void publishSuccess() {
        NotificationEvent event = NotificationEvent.builder()
                .type("BOOKING").recipientId(10L)
                .title("Booking Confirmed").message("Your spot is reserved")
                .channel("IN_APP").build();

        producer.publish(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY),
                eq(event));
    }

    @Test
    @DisplayName("should not propagate exception on RabbitMQ failure")
    void publishError() {
        NotificationEvent event = NotificationEvent.builder()
                .type("BOOKING").recipientId(10L)
                .title("Test").message("Test").build();

        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        // Should not throw — producer catches and logs
        assertThatCode(() -> producer.publish(event)).doesNotThrowAnyException();
    }
}
