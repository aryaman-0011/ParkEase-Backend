package com.parkease.booking_service.entity;

import com.parkease.booking_service.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BookingTest {
    @Test void builderAndGetters() {
        var now = LocalDateTime.now();
        var booking = Booking.builder().bookingId(1L).userId(10L).spotId(5L).lotId(2L)
                .lotName("Test Lot").spotNumber("A1").vehiclePlate("KA01").vehicleId(3L)
                .status(BookingStatus.RESERVED).scheduledStartTime(now).scheduledEndTime(now.plusHours(2))
                .pricePerHour(50.0).totalCost(100.0).build();
        assertEquals(1L, booking.getBookingId());
        assertEquals(10L, booking.getUserId());
        assertEquals(BookingStatus.RESERVED, booking.getStatus());
        assertEquals("A1", booking.getSpotNumber());
    }

    @Test void prePersistSetsTimestamps() {
        var booking = new Booking();
        booking.onCreate();
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getUpdatedAt());
    }

    @Test void preUpdateSetsUpdatedAt() {
        var booking = new Booking();
        booking.onUpdate();
        assertNotNull(booking.getUpdatedAt());
    }

    @Test void settersWork() {
        var booking = new Booking();
        booking.setBookingId(1L);
        booking.setUserId(10L);
        booking.setStatus(BookingStatus.ACTIVE);
        assertEquals(1L, booking.getBookingId());
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
    }
}
