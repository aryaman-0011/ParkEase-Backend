package com.parkease.booking_service.service;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.dto.ExtendBookingRequest;
import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long bookingId);

    List<BookingResponse> getBookingsByUser(Long userId);

    List<BookingResponse> getActiveBookingsForUser(Long userId);

    List<BookingResponse> getBookingsByLot(Long lotId);

    List<BookingResponse> getSpotSchedule(Long spotId);

    BookingResponse checkIn(Long bookingId);

    BookingResponse checkOut(Long bookingId);

    BookingResponse cancelBooking(Long bookingId);

    BookingResponse extendBooking(Long bookingId, ExtendBookingRequest request);
}
