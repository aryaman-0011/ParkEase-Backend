package com.parkease.payment_service.repository;

import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Payment entities.
 * Contains queries for user payments, booking payments, and revenue aggregation.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.userId = :userId AND p.status = 'SUCCESS'")
    Double getTotalSpentByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.bookingId IN " +
           "(SELECT b.bookingId FROM com.parkease.payment_service.entity.Payment b) " +
           "AND p.status = 'SUCCESS'")
    Double getTotalRevenue();
}
