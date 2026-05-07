package com.parkease.notification_service.repository;

import com.parkease.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderBySentAtDesc(Long recipientId);

    int countByRecipientIdAndIsReadFalse(Long recipientId);
}
