package com.parkease.spot_service.entity;

import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing a parking spot in the database.
 * Maps to the parking_spots table with type, status, and pricing.
 */
@Entity
@Table(name = "parking_spots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "spot_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spotId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "spot_number", nullable = false, length = 20)
    private String spotNumber;

    @Column(nullable = false)
    @Builder.Default
    private Integer floor = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 20)
    private SpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(name = "is_handicapped")
    @Builder.Default
    private Boolean isHandicapped = false;

    @Column(name = "is_ev_charging")
    @Builder.Default
    private Boolean isEVCharging = false;

    @Column(name = "price_per_hour")
    private Double pricePerHour;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
