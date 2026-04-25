package com.parkease.booking_service.service.impl;

import com.parkease.booking_service.dto.BookingResponse;
import com.parkease.booking_service.dto.CreateBookingRequest;
import com.parkease.booking_service.dto.ExtendBookingRequest;
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

// Core booking logic — handles time-slot based reservations with multi-vehicle support.
// Communicates with spot-service (status changes) and parkinglot-service (pricing) via REST.
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    // Inter-service URLs (resolved via Eureka service discovery)
    private static final String SPOT_SERVICE = "http://spot-service";
    private static final String LOT_SERVICE = "http://parkinglot-service";
    // Statuses considered "active" for conflict checks and dashboard display
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.RESERVED, BookingStatus.ACTIVE);

    // ── CREATE BOOKING ──
    // Creates a time-slot booking after validating: time range, vehicle availability, and spot conflicts.
    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        LocalDateTime start = request.getScheduledStartTime();
        LocalDateTime end = request.getScheduledEndTime();
        LocalDateTime now = LocalDateTime.now();

        // Validate time range
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new BadRequestException("End time must be after start time.");
        }
        if (start.isBefore(now.minusMinutes(5))) {
            throw new BadRequestException("Start time cannot be in the past.");
        }
        long durationMinutes = Duration.between(start, end).toMinutes();
        if (durationMinutes < 30) {
            throw new BadRequestException("Minimum booking duration is 30 minutes.");
        }

        // Prevent double-booking the same vehicle (one active booking per vehicle at a time)
        if (request.getVehicleId() != null) {
            var vehicleBookings = bookingRepository.findByVehicleIdAndStatusIn(
                    request.getVehicleId(), ACTIVE_STATUSES);
            if (!vehicleBookings.isEmpty()) {
                throw new BadRequestException(
                        "This vehicle already has an active booking. Complete or cancel it first.");
            }
        }

        // Check for overlapping bookings on this spot using: (start1 < end2) AND (start2 < end1)
        var conflicts = bookingRepository.findConflictingBookings(
                request.getSpotId(), start, end);
        if (!conflicts.isEmpty()) {
            Booking conflict = conflicts.get(0);
            throw new BadRequestException(
                    "This spot is already booked from " +
                    conflict.getScheduledStartTime().toLocalTime() + " to " +
                    conflict.getScheduledEndTime().toLocalTime() +
                    ". Please choose a different time or spot.");
        }

        // Fetch spot details from spot-service for pricing and status
        Map<String, Object> spotData = fetchSpotData(request.getSpotId());

        // Only block if someone is physically parked right now AND booking starts immediately
        // For future time slots, the conflict query above is the real authority
        String spotStatus = (String) spotData.get("status");
        if ("OCCUPIED".equals(spotStatus) && start.isBefore(now.plusMinutes(5))) {
            throw new BadRequestException("Spot is currently occupied right now. Choose a later start time.");
        }

        // Fetch lot details for pricing and lot name
        Map<String, Object> lotData = fetchLotData(request.getLotId());
        String lotName = lotData != null ? (String) lotData.get("name") : "Unknown Lot";
        // Use spot-level price if set, otherwise fall back to lot-level price
        Double pricePerHour = extractPrice(spotData, lotData);

        // If booking starts soon, try to mark the spot as RESERVED in spot-service
        // Non-fatal because the spot might already be RESERVED by another time-slot user
        if (start.isBefore(now.plusMinutes(15))) {
            try {
                restTemplate.put(SPOT_SERVICE + "/spots/" + request.getSpotId() + "/reserve", null);
            } catch (Exception e) {
                log.warn("Spot reserve call failed (may already be reserved): {}", e.getMessage());
            }
        }

        // Build and save the booking entity
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .spotId(request.getSpotId())
                .lotId(request.getLotId())
                .lotName(lotName)
                .spotNumber((String) spotData.get("spotNumber"))
                .vehiclePlate(request.getVehiclePlate())
                .vehicleId(request.getVehicleId())
                .status(BookingStatus.RESERVED)
                .scheduledStartTime(start)
                .scheduledEndTime(end)
                .pricePerHour(pricePerHour)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking {} created: user={} spot={} vehicle={} time={} to {}",
                booking.getBookingId(), request.getUserId(), request.getSpotId(),
                request.getVehicleId(), start, end);
        return toResponse(booking);
    }

    // ── EXTEND BOOKING ──
    // Extends the end time of an active/reserved booking after checking for conflicts
    @Override
    public BookingResponse extendBooking(Long bookingId, ExtendBookingRequest request) {
        Booking booking = findBooking(bookingId);

        // Only RESERVED or ACTIVE bookings can be extended
        if (booking.getStatus() != BookingStatus.RESERVED &&
            booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Can only extend RESERVED or ACTIVE bookings.");
        }

        LocalDateTime newEnd = request.getNewEndTime();
        if (newEnd.isBefore(booking.getScheduledEndTime()) || newEnd.isEqual(booking.getScheduledEndTime())) {
            throw new BadRequestException("New end time must be after current end time (" +
                    booking.getScheduledEndTime().toLocalTime() + ").");
        }

        // Check that the extended window doesn't overlap with other bookings (exclude self)
        var conflicts = bookingRepository.findConflictingBookingsExcluding(
                booking.getSpotId(),
                booking.getScheduledStartTime(),
                newEnd,
                bookingId);

        if (!conflicts.isEmpty()) {
            Booking conflict = conflicts.get(0);
            throw new BadRequestException(
                    "Cannot extend — spot is booked by another user from " +
                    conflict.getScheduledStartTime().toLocalTime() + " to " +
                    conflict.getScheduledEndTime().toLocalTime() +
                    ". You must vacate on time.");
        }

        booking.setScheduledEndTime(newEnd);
        booking = bookingRepository.save(booking);
        log.info("Booking {} extended to {}", bookingId, newEnd);
        return toResponse(booking);
    }

    // ── GET BOOKINGS ──

    // Get a single booking by ID
    @Override
    public BookingResponse getBookingById(Long bookingId) {
        return toResponse(findBooking(bookingId));
    }

    // Get all bookings for a user (includes past/cancelled), sorted newest first
    @Override
    public List<BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    // Get only active bookings for a user (RESERVED + ACTIVE) — used by dashboard
    @Override
    public List<BookingResponse> getActiveBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdAndStatusIn(userId, ACTIVE_STATUSES)
                .stream().map(this::toResponse).toList();
    }

    // Get all bookings in a lot — used by frontend to show booked time slots on spot cards
    @Override
    public List<BookingResponse> getBookingsByLot(Long lotId) {
        return bookingRepository.findByLotIdOrderByCreatedAtDesc(lotId)
                .stream().map(this::toResponse).toList();
    }

    // Get future bookings for a specific spot (schedule visualization)
    @Override
    public List<BookingResponse> getSpotSchedule(Long spotId) {
        return bookingRepository.findFutureBookingsForSpot(spotId, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    // ── CHECK-IN ──
    // Marks booking as ACTIVE and changes spot to OCCUPIED in spot-service
    @Override
    public BookingResponse checkIn(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BadRequestException("Can only check in from RESERVED status. Current: " + booking.getStatus());
        }

        // Don't allow check-in more than 15 minutes before scheduled start
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(booking.getScheduledStartTime().minusMinutes(15))) {
            throw new BadRequestException("Too early to check in. Your slot starts at " +
                    booking.getScheduledStartTime().toLocalTime());
        }

        // Tell spot-service to mark this spot as OCCUPIED
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/occupy", null);
        } catch (Exception e) {
            log.error("Failed to occupy spot: {}", e.getMessage());
            throw new BadRequestException("Could not update spot status.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setStartTime(now); // Record actual check-in time
        booking = bookingRepository.save(booking);
        log.info("Booking {} checked in", bookingId);
        return toResponse(booking);
    }

    // ── CHECK-OUT ──
    // Marks booking as COMPLETED, calculates final cost, releases spot back to AVAILABLE
    @Override
    public BookingResponse checkOut(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Can only check out from ACTIVE status. Current: " + booking.getStatus());
        }

        // Release the spot back to AVAILABLE in spot-service
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/release", null);
        } catch (Exception e) {
            log.error("Failed to release spot: {}", e.getMessage());
            throw new BadRequestException("Could not release spot.");
        }

        booking.setEndTime(LocalDateTime.now()); // Record actual check-out time
        booking.setStatus(BookingStatus.COMPLETED);

        // Calculate total cost based on scheduled duration (not actual), minimum 1 hour
        long minutes = Duration.between(booking.getScheduledStartTime(), booking.getScheduledEndTime()).toMinutes();
        double hours = Math.max(1, Math.ceil(minutes / 60.0));
        booking.setTotalCost(hours * booking.getPricePerHour());

        booking = bookingRepository.save(booking);
        log.info("Booking {} checked out. Total: ₹{}", bookingId, booking.getTotalCost());
        return toResponse(booking);
    }

    // ── CANCEL ──
    // Cancels a RESERVED booking and releases the spot
    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BadRequestException("Can only cancel RESERVED bookings. Current: " + booking.getStatus());
        }

        // Release the spot (non-fatal if it fails — spot may have already been released)
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/release", null);
        } catch (Exception e) {
            log.error("Failed to release spot on cancel: {}", e.getMessage());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setEndTime(LocalDateTime.now());
        booking.setTotalCost(0.0); // No charge for cancelled bookings
        booking = bookingRepository.save(booking);
        log.info("Booking {} cancelled", bookingId);
        return toResponse(booking);
    }

    // ── HELPER METHODS ──

    // Find a booking by ID or throw 404
    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    // Fetch spot details from spot-service via REST
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchSpotData(Long spotId) {
        try {
            return restTemplate.getForObject(SPOT_SERVICE + "/spots/" + spotId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch spot details: {}", e.getMessage());
            throw new BadRequestException("Could not fetch spot details. Please try again.");
        }
    }

    // Fetch lot details from parkinglot-service via REST (returns null on failure)
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchLotData(Long lotId) {
        try {
            return restTemplate.getForObject(LOT_SERVICE + "/lots/" + lotId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch lot details: {}", e.getMessage());
            return null;
        }
    }

    // Extract hourly price — prefers spot-level price, falls back to lot-level price
    private Double extractPrice(Map<String, Object> spotData, Map<String, Object> lotData) {
        if (spotData.get("pricePerHour") != null) {
            return ((Number) spotData.get("pricePerHour")).doubleValue();
        }
        if (lotData != null && lotData.get("pricePerHour") != null) {
            return ((Number) lotData.get("pricePerHour")).doubleValue();
        }
        return 0.0;
    }

    // Map Booking entity to BookingResponse DTO
    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .bookingId(b.getBookingId())
                .userId(b.getUserId())
                .spotId(b.getSpotId())
                .lotId(b.getLotId())
                .lotName(b.getLotName())
                .spotNumber(b.getSpotNumber())
                .vehiclePlate(b.getVehiclePlate())
                .vehicleId(b.getVehicleId())
                .status(b.getStatus())
                .scheduledStartTime(b.getScheduledStartTime())
                .scheduledEndTime(b.getScheduledEndTime())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .pricePerHour(b.getPricePerHour())
                .totalCost(b.getTotalCost())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
