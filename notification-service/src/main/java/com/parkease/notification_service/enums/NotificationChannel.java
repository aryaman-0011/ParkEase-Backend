package com.parkease.notification_service.enums;

// Channel through which the notification is delivered
public enum NotificationChannel {
    APP,    // In-app notification (stored in DB, shown in UI)
    EMAIL,  // Email notification via SMTP
    SMS     // SMS (placeholder for future)
}
