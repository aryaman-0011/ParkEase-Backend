package com.parkease.notification_service.service;

import com.parkease.notification_service.dto.NotificationEvent;
import com.parkease.notification_service.dto.NotificationResponse;

import java.util.List;

// Core notification operations
public interface NotificationService {

    // Save a notification from a RabbitMQ event or direct REST call
    NotificationResponse createFromEvent(NotificationEvent event);

    // Get all notifications for a user (newest first)
    List<NotificationResponse> getByRecipient(Long recipientId);

    // Get unread count for bell badge
    int getUnreadCount(Long recipientId);

    // Mark single notification as read
    void markAsRead(Long notificationId);

    // Mark all notifications as read for a user
    void markAllRead(Long recipientId);

    // Delete a notification
    void deleteNotification(Long notificationId);

    // Broadcast a notification to multiple users at once
    int broadcastToUsers(java.util.List<Long> recipientIds, String title, String message);
}
