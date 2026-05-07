package com.parkease.notification_service.service.impl;

import com.parkease.notification_service.dto.NotificationEvent;
import com.parkease.notification_service.dto.NotificationResponse;
import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.enums.NotificationChannel;
import com.parkease.notification_service.enums.NotificationType;
import com.parkease.notification_service.repository.NotificationRepository;
import com.parkease.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse createFromEvent(NotificationEvent event) {
        Notification notification = Notification.builder()
                .recipientId(event.getRecipientId())
                .type(parseType(event.getType()))
                .title(event.getTitle())
                .message(event.getMessage())
                .channel(parseChannel(event.getChannel()))
                .relatedId(event.getRelatedId())
                .relatedType(event.getRelatedType())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved: id={} type={} recipient={}", saved.getId(), saved.getType(), saved.getRecipientId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    public void markAllRead(Long recipientId) {
        notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId).forEach(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public int broadcastToUsers(List<Long> recipientIds, String title, String message) {
        List<Notification> notifications = recipientIds.stream()
                .map(id -> Notification.builder()
                        .recipientId(id)
                        .type(NotificationType.BROADCAST)
                        .title(title)
                        .message(message)
                        .channel(NotificationChannel.APP)
                        .relatedType("BROADCAST")
                        .build())
                .toList();
        List<Notification> saved = notificationRepository.saveAll(notifications);
        log.info("Broadcast sent to {} users: '{}'", saved.size(), title);
        return saved.size();
    }

    // --- Helpers ---

    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type);
        } catch (Exception e) {
            return NotificationType.PROMO;
        }
    }

    private NotificationChannel parseChannel(String channel) {
        try {
            return channel != null ? NotificationChannel.valueOf(channel) : NotificationChannel.APP;
        } catch (Exception e) {
            return NotificationChannel.APP;
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel().name())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .read(n.isRead())
                .sentAt(n.getSentAt())
                .build();
    }
}
