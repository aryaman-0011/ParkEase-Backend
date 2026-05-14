package com.parkease.vehicle_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating an existing vehicle.
 * All fields are optional; only provided fields are updated.
 */
@Data
public class UpdateVehicleRequest {

    @Size(min = 1, max = 50, message = "Make must be 1-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\s\\-]{0,48}$", message = "Make contains invalid characters")
    private String make;

    @Size(min = 1, max = 50, message = "Model must be 1-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\s\\-]{0,48}$", message = "Model contains invalid characters")
    private String model;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    @Pattern(regexp = "^$|^[A-Za-z][A-Za-z\\s\\-]{0,28}[A-Za-z]$", message = "Color must contain only letters")
    private String color;

    @Pattern(regexp = "^(2W|4W|HEAVY)$", message = "vehicleType must be 2W, 4W, or HEAVY")
    private String vehicleType;

    private Boolean isEV;
    private Boolean isActive;
}
