package com.parkease.notification_service.entity;

import com.parkease.notification_service.enums.NotificationChannel;
import com.parkease.notification_service.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// JPA entity — stored in parkease_notifications DB
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient", columnList = "recipientId"),
        @Index(name = "idx_recipient_read", columnList = "recipientId, isRead")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    private Long relatedId;       // bookingId, lotId, paymentId, etc.
    private String relatedType;   // "BOOKING", "LOT", "PAYMENT"

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;
}
