package com.parkease.parkinglot_service.event;

import lombok.*;
import java.io.Serializable;

// DTO sent to RabbitMQ — matches the notification-service's NotificationEvent
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {
    private Long recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private Long relatedId;
    private String relatedType;
}
