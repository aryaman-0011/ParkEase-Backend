package com.parkease.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_logs", indexes = {
        @Index(name = "idx_lot_timestamp", columnList = "lotId, timestamp"),
        @Index(name = "idx_lot_id", columnList = "lotId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private Long spotId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Current occupancy rate at the time of this snapshot (0.0 – 1.0) */
    @Column(nullable = false)
    private Double occupancyRate;

    @Column(nullable = false)
    private Integer availableSpots;

    @Column(nullable = false)
    private Integer totalSpots;

    /** Type of vehicle that triggered this log entry */
    @Column(length = 30)
    private String vehicleType;
}
