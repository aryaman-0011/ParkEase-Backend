package com.parkease.booking_service.repository;

import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByLotIdOrderByCreatedAtDesc(Long lotId);

    /** Find all active/reserved bookings for a user (multiple allowed now) */
    List<Booking> findByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);

    /** Find active/reserved bookings for a specific vehicle */
    List<Booking> findByVehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);

    List<Booking> findBySpotIdAndStatusIn(Long spotId, List<BookingStatus> statuses);

    long countByLotIdAndStatus(Long lotId, BookingStatus status);

    /**
     * Find bookings that conflict with a given time range on a specific spot.
     * Two ranges overlap when: existingStart < requestedEnd AND existingEnd > requestedStart
     */
    @Query("SELECT b FROM Booking b WHERE b.spotId = :spotId " +
           "AND b.status IN ('RESERVED', 'ACTIVE') " +
           "AND b.scheduledStartTime < :end " +
           "AND b.scheduledEndTime > :start")
    List<Booking> findConflictingBookings(
            @Param("spotId") Long spotId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Same as above but exclude a specific booking (used for extend check).
     */
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

    /** Get all future bookings for a spot (schedule view) */
    @Query("SELECT b FROM Booking b WHERE b.spotId = :spotId " +
           "AND b.status IN ('RESERVED', 'ACTIVE') " +
           "AND b.scheduledEndTime > :now " +
           "ORDER BY b.scheduledStartTime ASC")
    List<Booking> findFutureBookingsForSpot(
            @Param("spotId") Long spotId,
            @Param("now") LocalDateTime now);
}
