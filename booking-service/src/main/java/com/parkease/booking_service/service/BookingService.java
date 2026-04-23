package com.parkease.booking_service.service;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long bookingId);

    List<BookingResponse> getBookingsByUser(Long userId);

    BookingResponse getActiveBookingForUser(Long userId);

    List<BookingResponse> getBookingsByLot(Long lotId);

    BookingResponse checkIn(Long bookingId);

    BookingResponse checkOut(Long bookingId);

    BookingResponse cancelBooking(Long bookingId);
}
