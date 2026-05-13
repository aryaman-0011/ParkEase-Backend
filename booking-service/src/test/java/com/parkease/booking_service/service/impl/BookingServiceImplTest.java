package com.parkease.booking_service.service.impl;

import com.parkease.booking_service.client.ParkingLotServiceClient;
import com.parkease.booking_service.client.SpotServiceClient;
import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.dto.ExtendBookingRequest;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.event.NotificationEventProducer;
import com.parkease.booking_service.exception.BadRequestException;
import com.parkease.booking_service.exception.ResourceNotFoundException;
import com.parkease.booking_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SpotServiceClient spotServiceClient;
    @Mock private ParkingLotServiceClient parkingLotServiceClient;
    @Mock private NotificationEventProducer notificationProducer;
    @Mock private RedisLockRegistry redisLockRegistry;
    @Mock private org.springframework.integration.support.locks.DistributedLock mockLock;
    @InjectMocks private BookingServiceImpl bookingService;

    private Booking reservedBooking;
    private Booking activeBooking;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        // Default lock behavior — always acquire successfully
        lenient().when(redisLockRegistry.obtain(anyString())).thenReturn(mockLock);
        try { lenient().when(mockLock.tryLock(anyLong(), any())).thenReturn(true); } catch (Exception ignored) {}
        reservedBooking = Booking.builder()
                .bookingId(1L).userId(10L).spotId(100L).lotId(200L)
                .lotName("Downtown Parking").spotNumber("A-01")
                .vehiclePlate("MH12AB1234").vehicleId(50L)
                .status(BookingStatus.RESERVED)
                .scheduledStartTime(now.plusMinutes(30))
                .scheduledEndTime(now.plusHours(2))
                .pricePerHour(30.0)
                .createdAt(now).updatedAt(now).build();

        activeBooking = Booking.builder()
                .bookingId(2L).userId(10L).spotId(100L).lotId(200L)
                .lotName("Downtown Parking").spotNumber("A-01")
                .status(BookingStatus.ACTIVE)
                .scheduledStartTime(now.minusHours(1))
                .scheduledEndTime(now.plusHours(1))
                .startTime(now.minusHours(1))
                .pricePerHour(30.0)
                .createdAt(now).updatedAt(now).build();
    }

    private CreateBookingRequest buildCreateRequest(Long userId, Long spotId, Long lotId,
            LocalDateTime start, LocalDateTime end) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setUserId(userId);
        req.setSpotId(spotId);
        req.setLotId(lotId);
        req.setScheduledStartTime(start);
        req.setScheduledEndTime(end);
        return req;
    }

    private Map<String, Object> spotData(String status, Double pricePerHour) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("spotNumber", "A-01");
        if (pricePerHour != null) {
            data.put("pricePerHour", pricePerHour);
        }
        return data;
    }

    private Map<String, Object> lotData(Double pricePerHour, Long managerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Downtown Parking");
        data.put("pricePerHour", pricePerHour);
        if (managerId != null) {
            data.put("managerId", managerId);
        }
        return data;
    }

    private ExtendBookingRequest buildExtendRequest(LocalDateTime newEnd) {
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(newEnd);
        return req;
    }

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {
        @Test
        @DisplayName("should create booking and reserve nearby spot")
        void createSuccessReservesNearbySpot() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusMinutes(10), now.plusHours(2));
            req.setVehicleId(50L);
            req.setVehiclePlate("MH12AB1234");

            when(bookingRepository.findByVehicleIdAndStatusIn(eq(50L), anyList()))
                    .thenReturn(List.of());
            when(bookingRepository.findConflictingBookings(eq(100L), eq(req.getScheduledStartTime()),
                    eq(req.getScheduledEndTime()))).thenReturn(List.of());
            when(spotServiceClient.getSpotById(100L)).thenReturn(spotData("AVAILABLE", 45.0));
            when(parkingLotServiceClient.getLotById(200L)).thenReturn(lotData(30.0, 88L));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setBookingId(10L);
                return booking;
            });

            BookingResponse response = bookingService.createBooking(req);

            assertThat(response.getBookingId()).isEqualTo(10L);
            assertThat(response.getStatus()).isEqualTo(BookingStatus.RESERVED);
            assertThat(response.getLotName()).isEqualTo("Downtown Parking");
            assertThat(response.getSpotNumber()).isEqualTo("A-01");
            assertThat(response.getPricePerHour()).isEqualTo(45.0);
            verify(spotServiceClient).reserveSpot(100L);
            verify(notificationProducer, times(2)).publish(any());
            verify(mockLock).unlock();
        }

        @Test
        @DisplayName("should use lot price when spot price missing")
        void createUsesLotPriceFallback() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(3));

            when(bookingRepository.findConflictingBookings(eq(100L), eq(req.getScheduledStartTime()),
                    eq(req.getScheduledEndTime()))).thenReturn(List.of());
            when(spotServiceClient.getSpotById(100L)).thenReturn(spotData("AVAILABLE", null));
            when(parkingLotServiceClient.getLotById(200L)).thenReturn(lotData(25.0, null));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            BookingResponse response = bookingService.createBooking(req);

            assertThat(response.getPricePerHour()).isEqualTo(25.0);
            verify(spotServiceClient, never()).reserveSpot(100L);
            verify(notificationProducer).publish(any());
        }

        @Test
        @DisplayName("should fall back when lot service fails")
        void createFallsBackWhenLotServiceFails() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(2));

            when(bookingRepository.findConflictingBookings(eq(100L), eq(req.getScheduledStartTime()),
                    eq(req.getScheduledEndTime()))).thenReturn(List.of());
            when(spotServiceClient.getSpotById(100L)).thenReturn(spotData("AVAILABLE", 0.0));
            when(parkingLotServiceClient.getLotById(200L)).thenThrow(new RuntimeException("lot down"));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            BookingResponse response = bookingService.createBooking(req);

            assertThat(response.getLotName()).isEqualTo("Unknown Lot");
            assertThat(response.getPricePerHour()).isZero();
        }

        @Test
        @DisplayName("should reject when end time before start time")
        void endBeforeStart() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now); // end before start

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        @DisplayName("should reject when duration less than 30 minutes")
        void tooShort() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(1).plusMinutes(15));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Minimum booking duration");
        }

        @Test
        @DisplayName("should reject when vehicle already has active booking")
        void vehicleDoubleBook() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(3));
            req.setVehicleId(50L);

            when(bookingRepository.findByVehicleIdAndStatusIn(eq(50L), anyList()))
                    .thenReturn(List.of(reservedBooking));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already has an active booking");
        }

        @Test
        @DisplayName("should reject booking in the past")
        void startInPast() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.minusMinutes(10), now.plusHours(1));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("in the past");
        }

        @Test
        @DisplayName("should reject overlapping spot booking")
        void spotConflict() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(3));
            when(bookingRepository.findConflictingBookings(eq(100L), eq(req.getScheduledStartTime()),
                    eq(req.getScheduledEndTime()))).thenReturn(List.of(reservedBooking));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already booked");
        }

        @Test
        @DisplayName("should reject immediate booking for occupied spot")
        void occupiedSpotForImmediateBooking() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusMinutes(2), now.plusHours(1));
            when(bookingRepository.findConflictingBookings(eq(100L), eq(req.getScheduledStartTime()),
                    eq(req.getScheduledEndTime()))).thenReturn(List.of());
            when(spotServiceClient.getSpotById(100L)).thenReturn(spotData("OCCUPIED", 40.0));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("currently occupied");
        }

        @Test
        @DisplayName("should reject when spot service lookup fails")
        void spotLookupFailure() {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(2));
            when(bookingRepository.findConflictingBookings(eq(100L), eq(req.getScheduledStartTime()),
                    eq(req.getScheduledEndTime()))).thenReturn(List.of());
            when(spotServiceClient.getSpotById(100L)).thenThrow(new RuntimeException("spot down"));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Could not fetch spot details");
        }

        @Test
        @DisplayName("should reject when distributed lock is unavailable")
        void lockUnavailable() throws InterruptedException {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(2));
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("being booked by another user");

            verify(mockLock, never()).unlock();
        }

        @Test
        @DisplayName("should convert interrupted lock wait to bad request")
        void lockInterrupted() throws InterruptedException {
            CreateBookingRequest req = buildCreateRequest(10L, 100L, 200L,
                    now.plusHours(1), now.plusHours(2));
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new InterruptedException("stop"));

            assertThatThrownBy(() -> bookingService.createBooking(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("interrupted");

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("extendBooking")
    class ExtendBooking {
        @Test
        @DisplayName("should extend when no conflicts")
        void extendSuccess() {
            LocalDateTime newEnd = reservedBooking.getScheduledEndTime().plusHours(1);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));
            when(bookingRepository.findConflictingBookingsExcluding(anyLong(), any(), any(), anyLong()))
                    .thenReturn(List.of());
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.extendBooking(1L, buildExtendRequest(newEnd));

            assertThat(response.getScheduledEndTime()).isEqualTo(newEnd);
        }

        @Test
        @DisplayName("should reject when new end time is before current end")
        void extendEarlier() {
            LocalDateTime earlierEnd = reservedBooking.getScheduledEndTime().minusMinutes(30);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));

            assertThatThrownBy(() -> bookingService.extendBooking(1L, buildExtendRequest(earlierEnd)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("after current end time");
        }

        @Test
        @DisplayName("should reject for COMPLETED booking")
        void extendCompleted() {
            reservedBooking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));

            assertThatThrownBy(() -> bookingService.extendBooking(1L, buildExtendRequest(now.plusHours(5))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("RESERVED or ACTIVE");
        }

        @Test
        @DisplayName("should reject when extension conflicts")
        void extendConflict() {
            LocalDateTime newEnd = reservedBooking.getScheduledEndTime().plusHours(1);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));
            when(bookingRepository.findConflictingBookingsExcluding(eq(100L), eq(reservedBooking.getScheduledStartTime()),
                    eq(newEnd), eq(1L))).thenReturn(List.of(activeBooking));

            assertThatThrownBy(() -> bookingService.extendBooking(1L, buildExtendRequest(newEnd)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot extend");
        }
    }

    @Nested
    @DisplayName("getBookingById")
    class GetBookingById {
        @Test
        @DisplayName("should return booking when found")
        void found() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));

            BookingResponse response = bookingService.getBookingById(1L);

            assertThat(response.getBookingId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(BookingStatus.RESERVED);
        }

        @Test
        @DisplayName("should throw when not found")
        void notFound() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBookingById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getBookingsByUser")
    class GetByUser {
        @Test
        @DisplayName("should return all user bookings")
        void byUser() {
            when(bookingRepository.findByUserIdOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(reservedBooking, activeBooking));

            List<BookingResponse> result = bookingService.getBookingsByUser(10L);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getActiveBookingsForUser")
    class GetActive {
        @Test
        @DisplayName("should return only active bookings")
        void active() {
            when(bookingRepository.findByUserIdAndStatusIn(eq(10L), anyList()))
                    .thenReturn(List.of(reservedBooking));

            assertThat(bookingService.getActiveBookingsForUser(10L)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("lot and spot queries")
    class LotAndSpotQueries {
        @Test
        @DisplayName("should return bookings by lot")
        void byLot() {
            when(bookingRepository.findByLotIdOrderByCreatedAtDesc(200L))
                    .thenReturn(List.of(reservedBooking));

            assertThat(bookingService.getBookingsByLot(200L)).hasSize(1);
        }

        @Test
        @DisplayName("should return future schedule for spot")
        void spotSchedule() {
            when(bookingRepository.findFutureBookingsForSpot(eq(100L), any(LocalDateTime.class)))
                    .thenReturn(List.of(reservedBooking));

            assertThat(bookingService.getSpotSchedule(100L)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("checkIn")
    class CheckIn {
        @Test
        @DisplayName("should check in from RESERVED status")
        void checkInSuccess() {
            reservedBooking.setScheduledStartTime(now.minusMinutes(5)); // within window
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.checkIn(1L);

            assertThat(response.getStatus()).isEqualTo(BookingStatus.ACTIVE);
            assertThat(reservedBooking.getStartTime()).isNotNull();
            verify(spotServiceClient).occupySpot(100L);
        }

        @Test
        @DisplayName("should reject check-in from ACTIVE status")
        void checkInFromActive() {
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(activeBooking));

            assertThatThrownBy(() -> bookingService.checkIn(2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("RESERVED");
        }

        @Test
        @DisplayName("should reject too-early check-in")
        void checkInTooEarly() {
            reservedBooking.setScheduledStartTime(now.plusHours(2)); // far in future
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));

            assertThatThrownBy(() -> bookingService.checkIn(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Too early");
        }

        @Test
        @DisplayName("should reject when spot cannot be occupied")
        void occupySpotFails() {
            reservedBooking.setScheduledStartTime(now.minusMinutes(5));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));
            doThrow(new RuntimeException("spot down")).when(spotServiceClient).occupySpot(100L);

            assertThatThrownBy(() -> bookingService.checkIn(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Could not update spot status");
        }
    }

    @Nested
    @DisplayName("checkOut")
    class CheckOut {
        @Test
        @DisplayName("should check out from ACTIVE status and calculate cost")
        void checkOutSuccess() {
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(activeBooking));
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.checkOut(2L);

            assertThat(response.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            assertThat(response.getTotalCost()).isGreaterThan(0);
            verify(spotServiceClient).releaseSpot(100L);
        }

        @Test
        @DisplayName("should reject checkout from RESERVED status")
        void checkOutFromReserved() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));

            assertThatThrownBy(() -> bookingService.checkOut(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("should reject when spot cannot be released")
        void releaseSpotFails() {
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(activeBooking));
            doThrow(new RuntimeException("spot down")).when(spotServiceClient).releaseSpot(100L);

            assertThatThrownBy(() -> bookingService.checkOut(2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Could not release spot");
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class Cancel {
        @Test
        @DisplayName("should cancel RESERVED booking with zero cost")
        void cancelSuccess() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.cancelBooking(1L);

            assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(response.getTotalCost()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should reject cancelling ACTIVE booking")
        void cancelActive() {
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(activeBooking));

            assertThatThrownBy(() -> bookingService.cancelBooking(2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("RESERVED");
        }

        @Test
        @DisplayName("should still cancel when release fails")
        void releaseFailureStillCancels() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(reservedBooking));
            doThrow(new RuntimeException("spot down")).when(spotServiceClient).releaseSpot(100L);
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingService.cancelBooking(1L);

            assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(response.getTotalCost()).isZero();
        }
    }
}
