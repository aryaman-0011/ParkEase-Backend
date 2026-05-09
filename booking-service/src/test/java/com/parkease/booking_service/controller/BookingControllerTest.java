package com.parkease.booking_service.controller;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.dto.ExtendBookingRequest;
import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock private BookingService bookingService;
    @InjectMocks private BookingController controller;

    private BookingResponse sample() {
        return BookingResponse.builder().bookingId(1L).userId(10L).spotId(5L).vehicleId(3L)
                .lotId(2L).status(BookingStatus.RESERVED).totalCost(100.0)
                .scheduledStartTime(LocalDateTime.now()).scheduledEndTime(LocalDateTime.now().plusHours(2)).build();
    }

    @Test @DisplayName("createBooking returns 201")
    void create() {
        when(bookingService.createBooking(any())).thenReturn(sample());
        var resp = controller.createBooking(new CreateBookingRequest());
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getBookingId());
    }

    @Test @DisplayName("getBooking returns 200")
    void get() {
        when(bookingService.getBookingById(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.getBooking(1L).getStatusCode());
    }

    @Test @DisplayName("getUserBookings returns list")
    void userBookings() {
        when(bookingService.getBookingsByUser(10L)).thenReturn(List.of(sample()));
        assertEquals(1, controller.getUserBookings(10L).getBody().size());
    }

    @Test void active() {
        when(bookingService.getActiveBookingsForUser(10L)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getActiveBookings(10L).getStatusCode());
    }

    @Test void lot() {
        when(bookingService.getBookingsByLot(2L)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getLotBookings(2L).getStatusCode());
    }

    @Test void schedule() {
        when(bookingService.getSpotSchedule(5L)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getSpotSchedule(5L).getStatusCode());
    }

    @Test void checkIn() {
        when(bookingService.checkIn(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.checkIn(1L).getStatusCode());
    }

    @Test void checkOut() {
        when(bookingService.checkOut(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.checkOut(1L).getStatusCode());
    }

    @Test void cancel() {
        when(bookingService.cancelBooking(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.cancel(1L).getStatusCode());
    }

    @Test void extend() {
        when(bookingService.extendBooking(eq(1L), any())).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.extendBooking(1L, new ExtendBookingRequest()).getStatusCode());
    }
}
