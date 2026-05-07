package com.parkease.booking_service.event;

import com.parkease.booking_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Async
    public void publish(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.info("Published notification: type={} recipient={}", event.getType(), event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to publish notification: {}", e.getMessage());
        }
    }
}
