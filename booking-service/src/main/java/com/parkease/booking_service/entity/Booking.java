package com.parkease.booking_service.entity;

import com.parkease.booking_service.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// JPA entity representing a parking booking in the database.
// Each booking ties a user + vehicle to a specific spot for a scheduled time window.
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId; // Auto-generated primary key

    @Column(nullable = false)
    private Long userId; // ID of the user who made the booking

    @Column(nullable = false)
    private Long spotId; // ID of the parking spot being reserved

    @Column(nullable = false)
    private Long lotId; // ID of the parking lot containing the spot

    private String lotName; // Denormalized lot name (avoids extra API call on read)

    private String spotNumber; // Denormalized spot number (e.g., "A-01")

    private String vehiclePlate; // License plate of the vehicle

    private Long vehicleId; // Foreign key to vehicle-service's vehicle

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status; // RESERVED → ACTIVE → COMPLETED (or CANCELLED)

    // User-chosen scheduled start time (when the slot begins)
    @Column(nullable = false)
    private LocalDateTime scheduledStartTime;

    // User-chosen scheduled end time (when the slot ends)
    @Column(nullable = false)
    private LocalDateTime scheduledEndTime;

    private LocalDateTime startTime; // Actual check-in time (set on check-in)

    private LocalDateTime endTime; // Actual check-out time (set on check-out or cancel)

    @Column(nullable = false)
    private Double pricePerHour; // Rate at time of booking (from spot or lot)

    private Double totalCost; // Calculated on checkout: hours × pricePerHour

    @Column(updatable = false)
    private LocalDateTime createdAt; // When the booking was created

    private LocalDateTime updatedAt; // Last modification timestamp

    // Auto-set timestamps on insert
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Auto-update timestamp on modification
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
