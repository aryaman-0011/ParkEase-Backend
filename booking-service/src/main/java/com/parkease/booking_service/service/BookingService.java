package com.parkease.booking_service.service;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.dto.ExtendBookingRequest;
import java.util.List;

// Service interface defining all booking business operations.
// Implemented by BookingServiceImpl which handles time-slot logic, conflict detection, and spot-service communication.
public interface BookingService {

    // Create a new time-slot booking with conflict validation
    BookingResponse createBooking(CreateBookingRequest request);

    // Get a single booking by its ID
    BookingResponse getBookingById(Long bookingId);

    // Get all bookings (past + active) for a user
    List<BookingResponse> getBookingsByUser(Long userId);

    // Get only active/reserved bookings for a user (dashboard display)
    List<BookingResponse> getActiveBookingsForUser(Long userId);

    // Get all bookings in a lot (for showing booked slots on spot cards)
    List<BookingResponse> getBookingsByLot(Long lotId);

    // Get future bookings for a specific spot (schedule view)
    List<BookingResponse> getSpotSchedule(Long spotId);

    // Check in — RESERVED → ACTIVE, marks spot as OCCUPIED
    BookingResponse checkIn(Long bookingId);

    // Check out — ACTIVE → COMPLETED, calculates cost, releases spot
    BookingResponse checkOut(Long bookingId);

    // Cancel — RESERVED → CANCELLED, releases spot
    BookingResponse cancelBooking(Long bookingId);

    // Extend a booking's end time with conflict check
    BookingResponse extendBooking(Long bookingId, ExtendBookingRequest request);
}
