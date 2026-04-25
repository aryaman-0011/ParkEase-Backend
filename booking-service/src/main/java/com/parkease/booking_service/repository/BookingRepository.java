package com.parkease.booking_service.repository;

import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// JPA repository for Booking entity — includes custom JPQL queries for conflict detection
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Get all bookings for a user, newest first
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Get all bookings in a lot, newest first
    List<Booking> findByLotIdOrderByCreatedAtDesc(Long lotId);

    // Get active/reserved bookings for a user (supports multiple simultaneous bookings)
    List<Booking> findByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);

    // Get active/reserved bookings for a specific vehicle (to prevent double-booking one car)
    List<Booking> findByVehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);

    // Get active/reserved bookings for a spot
    List<Booking> findBySpotIdAndStatusIn(Long spotId, List<BookingStatus> statuses);

    // Count bookings by lot and status (used for analytics)
    long countByLotIdAndStatus(Long lotId, BookingStatus status);

    // Find bookings that overlap with a requested time range on a specific spot.
    // Overlap logic: two ranges overlap when (existingStart < requestedEnd) AND (existingEnd > requestedStart)
    @Query("SELECT b FROM Booking b WHERE b.spotId = :spotId " +
           "AND b.status IN ('RESERVED', 'ACTIVE') " +
           "AND b.scheduledStartTime < :end " +
           "AND b.scheduledEndTime > :start")
    List<Booking> findConflictingBookings(
            @Param("spotId") Long spotId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Same overlap check but excludes a specific booking — used when extending a booking
    // to ensure the extended window doesn't conflict with OTHER bookings on the same spot
    @Query("SELECT b FROM Booking b WHERE b.spotId = :spotId " +
           "AND b.bookingId <> :excludeId " +
           "AND b.status IN ('RESERVED', 'ACTIVE') " +
           "AND b.scheduledStartTime < :end " +
           "AND b.scheduledEndTime > :start")
    List<Booking> findConflictingBookingsExcluding(
            @Param("spotId") Long spotId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);

    // Get all future (not-yet-ended) bookings for a spot, sorted by start time
    // Used by the frontend to display booked time slots on spot cards
    @Query("SELECT b FROM Booking b WHERE b.spotId = :spotId " +
           "AND b.status IN ('RESERVED', 'ACTIVE') " +
           "AND b.scheduledEndTime > :now " +
           "ORDER BY b.scheduledStartTime ASC")
    List<Booking> findFutureBookingsForSpot(
            @Param("spotId") Long spotId,
            @Param("now") LocalDateTime now);
}
