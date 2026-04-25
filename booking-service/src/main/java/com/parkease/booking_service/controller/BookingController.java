package com.parkease.booking_service.controller;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.dto.ExtendBookingRequest;
import com.parkease.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    /** Returns ALL active bookings for user (multiple vehicles) */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<BookingResponse>> getActiveBookings(@PathVariable Long userId) {
        List<BookingResponse> active = bookingService.getActiveBookingsForUser(userId);
        return ResponseEntity.ok(active);
    }

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<BookingResponse>> getLotBookings(@PathVariable Long lotId) {
        return ResponseEntity.ok(bookingService.getBookingsByLot(lotId));
    }

    /** Get future booking schedule for a specific spot */
    @GetMapping("/spot/{spotId}/schedule")
    public ResponseEntity<List<BookingResponse>> getSpotSchedule(@PathVariable Long spotId) {
        return ResponseEntity.ok(bookingService.getSpotSchedule(spotId));
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<BookingResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkIn(id));
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<BookingResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkOut(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    /** Extend booking end time */
    @PutMapping("/{id}/extend")
    public ResponseEntity<BookingResponse> extendBooking(
            @PathVariable Long id,
            @Valid @RequestBody ExtendBookingRequest request) {
        return ResponseEntity.ok(bookingService.extendBooking(id, request));
    }
}
