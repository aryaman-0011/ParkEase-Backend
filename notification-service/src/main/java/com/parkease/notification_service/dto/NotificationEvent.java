package com.parkease.notification_service.dto;

import lombok.*;
import java.io.Serializable;

// DTO published to RabbitMQ by producer services and received by the consumer.
// Also used for direct REST-based notification sending.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {
    private Long recipientId;       // User ID to notify
    private String type;            // NotificationType name (e.g. "BOOKING", "LOT_APPROVED")
    private String title;           // "Booking Confirmed"
    private String message;         // "Spot A-01 reserved at Downtown Parking"
    private String channel;         // "APP", "EMAIL" (defaults to APP)
    private Long relatedId;         // bookingId, lotId, etc.
    private String relatedType;     // "BOOKING", "LOT", "PAYMENT"
}
