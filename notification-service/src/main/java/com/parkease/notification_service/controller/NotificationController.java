package com.parkease.notification_service.controller;

import com.parkease.notification_service.dto.BroadcastRequest;
import com.parkease.notification_service.dto.NotificationEvent;
import com.parkease.notification_service.dto.NotificationResponse;
import com.parkease.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// REST controller for notification operations.
// The frontend polls these endpoints to display notifications.
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Send a notification directly (bypasses RabbitMQ — useful for testing)
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(@RequestBody NotificationEvent event) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createFromEvent(event));
    }

    // Get all notifications for a user (frontend polls this)
    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationResponse>> getByRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    // Get unread count for bell badge
    @GetMapping("/unread-count/{recipientId}")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(recipientId)));
    }

    // Mark a single notification as read
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    // Mark all notifications as read for a user
    @PatchMapping("/read-all/{recipientId}")
    public ResponseEntity<Void> markAllRead(@PathVariable Long recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok().build();
    }

    // Delete a notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    // Broadcast a notification to multiple users at once (admin feature)
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody BroadcastRequest request) {
        int count = notificationService.broadcastToUsers(
                request.getRecipientIds(),
                request.getTitle(),
                request.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Broadcast sent successfully", "recipientCount", count));
    }
}
