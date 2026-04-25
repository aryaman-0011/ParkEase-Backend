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

// REST controller for time-slot based parking bookings — create, check-in/out, cancel, extend
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // Create a new time-slot booking for a specific spot and vehicle
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    // Get a single booking by its ID
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    // Get all bookings (past + active) for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    // Get only ACTIVE/RESERVED bookings for a user (supports multiple vehicles)
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<BookingResponse>> getActiveBookings(@PathVariable Long userId) {
        List<BookingResponse> active = bookingService.getActiveBookingsForUser(userId);
        return ResponseEntity.ok(active);
    }

    // Get all bookings in a lot — used by frontend to show booked time slots on spot cards
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<BookingResponse>> getLotBookings(@PathVariable Long lotId) {
        return ResponseEntity.ok(bookingService.getBookingsByLot(lotId));
    }

    // Get future booking schedule for a specific spot (for conflict visualization)
    @GetMapping("/spot/{spotId}/schedule")
    public ResponseEntity<List<BookingResponse>> getSpotSchedule(@PathVariable Long spotId) {
        return ResponseEntity.ok(bookingService.getSpotSchedule(spotId));
    }

    // Check in — marks booking as ACTIVE, changes spot status to OCCUPIED
    @PutMapping("/{id}/checkin")
    public ResponseEntity<BookingResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkIn(id));
    }

    // Check out — marks booking as COMPLETED, calculates final cost, releases spot
    @PutMapping("/{id}/checkout")
    public ResponseEntity<BookingResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkOut(id));
    }

    // Cancel a RESERVED booking — releases the spot back to AVAILABLE
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    // Extend a booking's end time (with conflict check against future reservations)
    @PutMapping("/{id}/extend")
    public ResponseEntity<BookingResponse> extendBooking(
            @PathVariable Long id,
            @Valid @RequestBody ExtendBookingRequest request) {
        return ResponseEntity.ok(bookingService.extendBooking(id, request));
    }
}
