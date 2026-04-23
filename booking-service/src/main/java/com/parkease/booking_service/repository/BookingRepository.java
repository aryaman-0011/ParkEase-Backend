package com.parkease.booking_service.repository;

import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByLotIdOrderByCreatedAtDesc(Long lotId);

    Optional<Booking> findByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);

    List<Booking> findBySpotIdAndStatusIn(Long spotId, List<BookingStatus> statuses);

    long countByLotIdAndStatus(Long lotId, BookingStatus status);
}
