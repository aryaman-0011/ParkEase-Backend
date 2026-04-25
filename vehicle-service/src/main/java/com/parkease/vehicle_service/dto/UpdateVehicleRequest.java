package com.parkease.vehicle_service.dto;

import lombok.Data;

/**
 * Request DTO for updating an existing vehicle.
 * All fields are optional; only provided fields are updated.
 */
@Data
public class UpdateVehicleRequest {
    private String make;
    private String model;
    private String color;
    private String vehicleType;
    private Boolean isEV;
    private Boolean isActive;
}
