package com.parkease.booking_service.entity;

import com.parkease.booking_service.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long spotId;

    @Column(nullable = false)
    private Long lotId;

    private String lotName;

    private String spotNumber;

    private String vehiclePlate;

    /** Links to vehicle-service vehicle */
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    /** User-chosen scheduled start time */
    @Column(nullable = false)
    private LocalDateTime scheduledStartTime;

    /** User-chosen scheduled end time */
    @Column(nullable = false)
    private LocalDateTime scheduledEndTime;

    /** Actual check-in time */
    private LocalDateTime startTime;

    /** Actual check-out time */
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double pricePerHour;

    private Double totalCost;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
