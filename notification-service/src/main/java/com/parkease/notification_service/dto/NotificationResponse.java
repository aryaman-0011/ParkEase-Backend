package com.parkease.notification_service.dto;

import lombok.*;
import java.time.LocalDateTime;

// Response DTO returned by REST endpoints
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private Long relatedId;
    private String relatedType;
    private boolean read;
    private LocalDateTime sentAt;
}
