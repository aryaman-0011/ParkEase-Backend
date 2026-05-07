package com.parkease.notification_service.service.impl;

import com.parkease.notification_service.dto.NotificationEvent;
import com.parkease.notification_service.dto.NotificationResponse;
import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.enums.NotificationChannel;
import com.parkease.notification_service.enums.NotificationType;
import com.parkease.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id(1L)
                .recipientId(100L)
                .type(NotificationType.BOOKING)
                .title("Booking Confirmed")
                .message("Spot A-01 reserved")
                .channel(NotificationChannel.APP)
                .relatedId(50L)
                .relatedType("BOOKING")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createFromEvent")
    class CreateFromEvent {
        @Test
        @DisplayName("should create notification from event with valid type")
        void createWithValidType() {
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(100L).type("BOOKING").title("Booking Confirmed")
                    .message("Spot A-01 reserved").channel("APP")
                    .relatedId(50L).relatedType("BOOKING").build();

            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            NotificationResponse response = notificationService.createFromEvent(event);

            assertThat(response.getTitle()).isEqualTo("Booking Confirmed");
            assertThat(response.getType()).isEqualTo("BOOKING");
            assertThat(response.isRead()).isFalse();

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getRecipientId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should default to PROMO for unknown type")
        void createWithUnknownType() {
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(100L).type("UNKNOWN_TYPE").title("Test")
                    .message("Test msg").build();

            Notification promoNotification = Notification.builder()
                    .id(2L).recipientId(100L).type(NotificationType.PROMO)
                    .title("Test").message("Test msg").channel(NotificationChannel.APP)
                    .sentAt(LocalDateTime.now()).build();
            when(notificationRepository.save(any())).thenReturn(promoNotification);

            NotificationResponse response = notificationService.createFromEvent(event);

            assertThat(response.getType()).isEqualTo("PROMO");
        }

        @Test
        @DisplayName("should default channel to APP when null")
        void createWithNullChannel() {
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(100L).type("BOOKING").title("Test")
                    .message("Msg").channel(null).build();

            when(notificationRepository.save(any())).thenReturn(sampleNotification);
            notificationService.createFromEvent(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getChannel()).isEqualTo(NotificationChannel.APP);
        }
    }

    @Nested
    @DisplayName("getByRecipient")
    class GetByRecipient {
        @Test
        @DisplayName("should return notifications sorted by sentAt desc")
        void getNotifications() {
            when(notificationRepository.findByRecipientIdOrderBySentAtDesc(100L))
                    .thenReturn(List.of(sampleNotification));

            List<NotificationResponse> result = notificationService.getByRecipient(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void getNotificationsEmpty() {
            when(notificationRepository.findByRecipientIdOrderBySentAtDesc(999L))
                    .thenReturn(List.of());

            assertThat(notificationService.getByRecipient(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {
        @Test
        @DisplayName("should return correct unread count")
        void unreadCount() {
            when(notificationRepository.countByRecipientIdAndIsReadFalse(100L)).thenReturn(5);

            assertThat(notificationService.getUnreadCount(100L)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {
        @Test
        @DisplayName("should mark notification as read")
        void markRead() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

            notificationService.markAsRead(1L);

            assertThat(sampleNotification.isRead()).isTrue();
            verify(notificationRepository).save(sampleNotification);
        }

        @Test
        @DisplayName("should do nothing if notification not found")
        void markReadNotFound() {
            when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

            notificationService.markAsRead(99L);

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAllRead")
    class MarkAllRead {
        @Test
        @DisplayName("should mark all unread notifications as read")
        void markAll() {
            Notification n2 = Notification.builder()
                    .id(2L).recipientId(100L).type(NotificationType.PAYMENT)
                    .title("Payment").message("Paid").channel(NotificationChannel.APP)
                    .isRead(false).sentAt(LocalDateTime.now()).build();

            when(notificationRepository.findByRecipientIdOrderBySentAtDesc(100L))
                    .thenReturn(List.of(sampleNotification, n2));

            notificationService.markAllRead(100L);

            assertThat(sampleNotification.isRead()).isTrue();
            assertThat(n2.isRead()).isTrue();
            verify(notificationRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("deleteNotification")
    class DeleteNotification {
        @Test
        @DisplayName("should delete by ID")
        void deleteById() {
            notificationService.deleteNotification(1L);
            verify(notificationRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("broadcastToUsers")
    class BroadcastToUsers {
        @Test
        @DisplayName("should create BROADCAST notification for each recipient")
        void broadcastSuccess() {
            List<Long> ids = List.of(1L, 2L, 3L);

            when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> {
                List<Notification> list = invocation.getArgument(0);
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setId((long) (i + 1));
                    list.get(i).setSentAt(LocalDateTime.now());
                }
                return list;
            });

            int count = notificationService.broadcastToUsers(ids, "Maintenance", "System down at 2AM");

            assertThat(count).isEqualTo(3);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationRepository).saveAll(captor.capture());
            List<Notification> saved = captor.getValue();

            assertThat(saved).hasSize(3);
            assertThat(saved).allMatch(n -> n.getType() == NotificationType.BROADCAST);
            assertThat(saved).allMatch(n -> n.getTitle().equals("Maintenance"));
            assertThat(saved).allMatch(n -> n.getRelatedType().equals("BROADCAST"));
            assertThat(saved.stream().map(Notification::getRecipientId).toList())
                    .containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should handle empty recipient list")
        void broadcastEmpty() {
            when(notificationRepository.saveAll(anyList())).thenReturn(List.of());

            int count = notificationService.broadcastToUsers(List.of(), "Test", "Msg");

            assertThat(count).isEqualTo(0);
        }
    }
}
