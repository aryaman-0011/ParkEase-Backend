package com.parkease.vehicle_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO representing a vehicle returned to the client.
 * Includes license plate, make/model, color, and owner information.
 */
@Data
@Builder
public class VehicleResponse {
    private Long vehicleId;
    private Long ownerId;
    private String licensePlate;
    private String make;
    private String model;
    private String color;
    private String vehicleType;
    private Boolean isEV;
    private LocalDate registeredAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
