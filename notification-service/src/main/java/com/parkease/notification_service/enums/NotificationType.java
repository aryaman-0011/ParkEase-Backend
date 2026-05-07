package com.parkease.notification_service.enums;

// Types of notifications — mapped to role-based events
public enum NotificationType {
    // Driver notifications
    BOOKING,            // Booking confirmed
    CHECKIN,            // Checked in
    CHECKOUT,           // Checked out
    PAYMENT,            // Payment processed
    EXPIRY,             // Booking expiry warning

    // Manager notifications
    LOT_APPROVED,       // Admin approved lot
    LOT_REJECTED,       // Admin rejected lot
    BOOKING_RECEIVED,   // Driver booked a spot in manager's lot

    // Admin notifications
    NEW_LOT_PENDING,    // Manager submitted lot for approval

    // All roles
    PROMO,              // System announcements
    BROADCAST           // Admin broadcast messages
}
