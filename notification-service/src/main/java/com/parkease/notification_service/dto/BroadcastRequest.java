package com.parkease.notification_service.dto;

import lombok.*;
import java.util.List;

// DTO for admin broadcast — sends a notification to multiple users at once
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastRequest {
    private List<Long> recipientIds;    // User IDs to notify
    private String title;               // Broadcast title
    private String message;             // Broadcast message body
}
