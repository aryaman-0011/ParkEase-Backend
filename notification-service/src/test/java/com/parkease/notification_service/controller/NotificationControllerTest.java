package com.parkease.notification_service.controller;

import com.parkease.notification_service.dto.*;
import com.parkease.notification_service.service.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock private NotificationService service;
    @InjectMocks private NotificationController controller;

    private NotificationResponse sample() {
        return NotificationResponse.builder().id(1L).recipientId(10L).title("Test")
                .message("Hello").type("BOOKING").read(false).sentAt(LocalDateTime.now()).build();
    }

    @Test void send() {
        when(service.createFromEvent(any())).thenReturn(sample());
        assertEquals(HttpStatus.CREATED, controller.send(new NotificationEvent()).getStatusCode());
    }
    @Test void getByRecipient() {
        when(service.getByRecipient(10L)).thenReturn(List.of(sample()));
        assertEquals(1, controller.getByRecipient(10L).getBody().size());
    }
    @Test void getUnreadCount() {
        when(service.getUnreadCount(10L)).thenReturn(5);
        assertEquals(5, controller.getUnreadCount(10L).getBody().get("count"));
    }
    @Test void markAsRead() {
        doNothing().when(service).markAsRead(1L);
        assertEquals(HttpStatus.OK, controller.markAsRead(1L).getStatusCode());
    }
    @Test void markAllRead() {
        doNothing().when(service).markAllRead(10L);
        assertEquals(HttpStatus.OK, controller.markAllRead(10L).getStatusCode());
    }
    @Test void deleteNotification() {
        doNothing().when(service).deleteNotification(1L);
        assertEquals(HttpStatus.OK, controller.deleteNotification(1L).getStatusCode());
    }
    @Test void broadcast() {
        when(service.broadcastToUsers(anyList(), anyString(), anyString())).thenReturn(3);
        BroadcastRequest req = new BroadcastRequest();
        req.setRecipientIds(List.of(1L, 2L, 3L)); req.setTitle("X"); req.setMessage("Y");
        var resp = controller.broadcast(req);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(3, resp.getBody().get("recipientCount"));
    }
}
