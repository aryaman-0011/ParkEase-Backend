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

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    private static final String SPOT_SERVICE = "http://spot-service";
    private static final String LOT_SERVICE = "http://parkinglot-service";
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.RESERVED, BookingStatus.ACTIVE);

    // ────────────────────────────────────────────────
    //  CREATE BOOKING (time-slot + multi-vehicle)
    // ────────────────────────────────────────────────
    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        LocalDateTime start = request.getScheduledStartTime();
        LocalDateTime end = request.getScheduledEndTime();
        LocalDateTime now = LocalDateTime.now();

        // ── Validation ──
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

        // ── Check: same vehicle not double-booked ──
        if (request.getVehicleId() != null) {
            var vehicleBookings = bookingRepository.findByVehicleIdAndStatusIn(
                    request.getVehicleId(), ACTIVE_STATUSES);
            if (!vehicleBookings.isEmpty()) {
                throw new BadRequestException(
                        "This vehicle already has an active booking. Complete or cancel it first.");
            }
        }

        // ── Check: no time-slot conflict on this spot ──
        var conflicts = bookingRepository.findConflictingBookings(
                request.getSpotId(), start, end);
        if (!conflicts.isEmpty()) {
            // Build a helpful message
            Booking conflict = conflicts.get(0);
            throw new BadRequestException(
                    "This spot is already booked from " +
                    conflict.getScheduledStartTime().toLocalTime() + " to " +
                    conflict.getScheduledEndTime().toLocalTime() +
                    ". Please choose a different time or spot.");
        }

        // ── Get spot details ──
        Map<String, Object> spotData = fetchSpotData(request.getSpotId());
        // For time-slot booking, the static spot status is secondary.
        // The conflict query above is the real authority.
        // Only block if someone is physically parked there right now AND
        // the new booking wants to start immediately.
        String spotStatus = (String) spotData.get("status");
        if ("OCCUPIED".equals(spotStatus) && start.isBefore(now.plusMinutes(5))) {
            throw new BadRequestException("Spot is currently occupied right now. Choose a later start time.");
        }

        // ── Get lot details ──
        Map<String, Object> lotData = fetchLotData(request.getLotId());
        String lotName = lotData != null ? (String) lotData.get("name") : "Unknown Lot";
        Double pricePerHour = extractPrice(spotData, lotData);

        // ── Try to reserve spot (non-fatal — it may already be reserved by another time-slot) ──
        if (start.isBefore(now.plusMinutes(15))) {
            try {
                restTemplate.put(SPOT_SERVICE + "/spots/" + request.getSpotId() + "/reserve", null);
            } catch (Exception e) {
                log.warn("Spot reserve call failed (may already be reserved): {}", e.getMessage());
                // Non-fatal: spot may be reserved by another booking's time slot
            }
        }

        // ── Create booking ──
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

    // ────────────────────────────────────────────────
    //  EXTEND BOOKING
    // ────────────────────────────────────────────────
    @Override
    public BookingResponse extendBooking(Long bookingId, ExtendBookingRequest request) {
        Booking booking = findBooking(bookingId);

        if (booking.getStatus() != BookingStatus.RESERVED &&
            booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Can only extend RESERVED or ACTIVE bookings.");
        }

        LocalDateTime newEnd = request.getNewEndTime();
        if (newEnd.isBefore(booking.getScheduledEndTime()) || newEnd.isEqual(booking.getScheduledEndTime())) {
            throw new BadRequestException("New end time must be after current end time (" +
                    booking.getScheduledEndTime().toLocalTime() + ").");
        }

        // Check for conflicts between current end and new end (exclude self)
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

    // ────────────────────────────────────────────────
    //  GET BOOKINGS
    // ────────────────────────────────────────────────
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
    public List<BookingResponse> getActiveBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdAndStatusIn(userId, ACTIVE_STATUSES)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BookingResponse> getBookingsByLot(Long lotId) {
        return bookingRepository.findByLotIdOrderByCreatedAtDesc(lotId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BookingResponse> getSpotSchedule(Long spotId) {
        return bookingRepository.findFutureBookingsForSpot(spotId, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    // ────────────────────────────────────────────────
    //  CHECK-IN
    // ────────────────────────────────────────────────
    @Override
    public BookingResponse checkIn(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BadRequestException("Can only check in from RESERVED status. Current: " + booking.getStatus());
        }

        // Allow check-in within a reasonable window (15 min early to scheduled end)
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(booking.getScheduledStartTime().minusMinutes(15))) {
            throw new BadRequestException("Too early to check in. Your slot starts at " +
                    booking.getScheduledStartTime().toLocalTime());
        }

        // Mark spot as occupied
        try {
            restTemplate.put(SPOT_SERVICE + "/spots/" + booking.getSpotId() + "/occupy", null);
        } catch (Exception e) {
            log.error("Failed to occupy spot: {}", e.getMessage());
            throw new BadRequestException("Could not update spot status.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setStartTime(now);
        booking = bookingRepository.save(booking);
        log.info("Booking {} checked in", bookingId);
        return toResponse(booking);
    }

    // ────────────────────────────────────────────────
    //  CHECK-OUT
    // ────────────────────────────────────────────────
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

        // Calculate total cost based on scheduled duration
        long minutes = Duration.between(booking.getScheduledStartTime(), booking.getScheduledEndTime()).toMinutes();
        double hours = Math.max(1, Math.ceil(minutes / 60.0));
        booking.setTotalCost(hours * booking.getPricePerHour());

        booking = bookingRepository.save(booking);
        log.info("Booking {} checked out. Total: ₹{}", bookingId, booking.getTotalCost());
        return toResponse(booking);
    }

    // ────────────────────────────────────────────────
    //  CANCEL
    // ────────────────────────────────────────────────
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

    // ────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchSpotData(Long spotId) {
        try {
            return restTemplate.getForObject(SPOT_SERVICE + "/spots/" + spotId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch spot details: {}", e.getMessage());
            throw new BadRequestException("Could not fetch spot details. Please try again.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchLotData(Long lotId) {
        try {
            return restTemplate.getForObject(LOT_SERVICE + "/lots/" + lotId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch lot details: {}", e.getMessage());
            return null;
        }
    }

    private Double extractPrice(Map<String, Object> spotData, Map<String, Object> lotData) {
        if (spotData.get("pricePerHour") != null) {
            return ((Number) spotData.get("pricePerHour")).doubleValue();
        }
        if (lotData != null && lotData.get("pricePerHour") != null) {
            return ((Number) lotData.get("pricePerHour")).doubleValue();
        }
        return 0.0;
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
