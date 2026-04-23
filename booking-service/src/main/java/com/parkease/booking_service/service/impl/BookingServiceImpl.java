package com.parkease.booking_service.service.impl;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import com.parkease.booking_service.exception.BadRequestException;
import com.parkease.booking_service.exception.ResourceNotFoundException;
import com.parkease.booking_service.repository.BookingRepository;
import com.parkease.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    private static final String SPOT_SERVICE = "http://spot-service";
    private static final String LOT_SERVICE = "http://parkinglot-service";

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        // Check if user already has an active booking
        var existing = bookingRepository.findByUserIdAndStatusIn(
                request.getUserId(),
                List.of(BookingStatus.RESERVED, BookingStatus.ACTIVE)
        );
        if (existing.isPresent()) {
            throw new BadRequestException("You already have an active booking. Complete or cancel it first.");
        }

        // Check if spot is already booked
        var spotBookings = bookingRepository.findBySpotIdAndStatusIn(
                request.getSpotId(),
                List.of(BookingStatus.RESERVED, BookingStatus.ACTIVE)
        );
        if (!spotBookings.isEmpty()) {
            throw new BadRequestException("This spot is already booked.");
        }

        // Get spot details from spot-service
        Map<String, Object> spotData;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(
                    SPOT_SERVICE + "/spots/" + request.getSpotId(), Map.class);
            spotData = resp;
        } catch (Exception e) {
            log.error("Failed to fetch spot details: {}", e.getMessage());
            throw new BadRequestException("Could not fetch spot details. Please try again.");
        }

        if (spotData == null) throw new BadRequestException("Spot not found.");
        String spotStatus = (String) spotData.get("status");
        if (!"AVAILABLE".equals(spotStatus)) {
            throw new BadRequestException("Spot is not available. Current status: " + spotStatus);
        }

        // Get lot details from parkinglot-service
        Map<String, Object> lotData;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(
                    LOT_SERVICE + "/lots/" + request.getLotId(), Map.class);
            lotData = resp;
        } catch (Exception e) {
            log.error("Failed to fetch lot details: {}", e.getMessage());
            throw new BadRequestException("Could not fetch lot details.");
        }

        String lotName = lotData != null ? (String) lotData.get("name") : "Unknown Lot";
        Double pricePerHour = spotData.get("pricePerHour") != null
                ? ((Number) spotData.get("pricePerHour")).doubleValue()
                : (lotData != null && lotData.get("pricePerHour") != null
                    ? ((Number) lotData.get("pricePerHour")).doubleValue()
                    : 0.0);

        // Reserve the spot via spot-service
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + request.getSpotId() + "/reserve", null);
        } catch (Exception e) {
            log.error("Failed to reserve spot: {}", e.getMessage());
            throw new BadRequestException("Could not reserve spot. Please try again.");
        }

        // Create the booking
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .spotId(request.getSpotId())
                .lotId(request.getLotId())
                .lotName(lotName)
                .spotNumber((String) spotData.get("spotNumber"))
                .vehiclePlate(request.getVehiclePlate())
                .status(BookingStatus.RESERVED)
                .startTime(LocalDateTime.now())
                .pricePerHour(pricePerHour)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created: {} for user {} at spot {}", booking.getBookingId(), request.getUserId(), request.getSpotId());
        return toResponse(booking);
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        return toResponse(findBooking(bookingId));
    }

    @Override
    public List<BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public BookingResponse getActiveBookingForUser(Long userId) {
        var booking = bookingRepository.findByUserIdAndStatusIn(
                userId, List.of(BookingStatus.RESERVED, BookingStatus.ACTIVE));
        return booking.map(this::toResponse).orElse(null);
    }

    @Override
    public List<BookingResponse> getBookingsByLot(Long lotId) {
        return bookingRepository.findByLotIdOrderByCreatedAtDesc(lotId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public BookingResponse checkIn(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BadRequestException("Can only check in from RESERVED status. Current: " + booking.getStatus());
        }

        // Mark spot as occupied
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/occupy", null);
        } catch (Exception e) {
            log.error("Failed to occupy spot: {}", e.getMessage());
            throw new BadRequestException("Could not update spot status.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setStartTime(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        log.info("Booking {} checked in", bookingId);
        return toResponse(booking);
    }

    @Override
    public BookingResponse checkOut(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Can only check out from ACTIVE status. Current: " + booking.getStatus());
        }

        // Release spot
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/release", null);
        } catch (Exception e) {
            log.error("Failed to release spot: {}", e.getMessage());
            throw new BadRequestException("Could not release spot.");
        }

        booking.setEndTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.COMPLETED);

        // Calculate total cost
        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        double hours = Math.max(1, Math.ceil(minutes / 60.0)); // minimum 1 hour
        booking.setTotalCost(hours * booking.getPricePerHour());

        booking = bookingRepository.save(booking);
        log.info("Booking {} checked out. Total: ₹{}", bookingId, booking.getTotalCost());
        return toResponse(booking);
    }

    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BadRequestException("Can only cancel RESERVED bookings. Current: " + booking.getStatus());
        }

        // Release spot
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/release", null);
        } catch (Exception e) {
            log.error("Failed to release spot on cancel: {}", e.getMessage());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setEndTime(LocalDateTime.now());
        booking.setTotalCost(0.0);
        booking = bookingRepository.save(booking);
        log.info("Booking {} cancelled", bookingId);
        return toResponse(booking);
    }

    // ── Helpers ──

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .bookingId(b.getBookingId())
                .userId(b.getUserId())
                .spotId(b.getSpotId())
                .lotId(b.getLotId())
                .lotName(b.getLotName())
                .spotNumber(b.getSpotNumber())
                .vehiclePlate(b.getVehiclePlate())
                .status(b.getStatus())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .pricePerHour(b.getPricePerHour())
                .totalCost(b.getTotalCost())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
