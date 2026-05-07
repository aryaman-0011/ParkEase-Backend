package com.parkease.notification_service.consumer;

import com.parkease.notification_service.dto.NotificationEvent;
import com.parkease.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// RabbitMQ consumer — listens to the notification queue and saves notifications.
// Producer services publish NotificationEvent with a real recipientId for each user.
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.queue")
    public void consume(NotificationEvent event) {
        log.info("RabbitMQ event received: type={} recipient={}", event.getType(), event.getRecipientId());
        try {
            notificationService.createFromEvent(event);
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }
}
