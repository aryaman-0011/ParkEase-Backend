package com.parkease.vehicle_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for registering a new vehicle.
 * Contains license plate, make, model, color, and vehicle type.
 */
@Data
public class RegisterVehicleRequest {

    @NotNull(message = "ownerId is required")
    private Long ownerId;

    @NotBlank(message = "licensePlate is required")
    @Pattern(regexp = "^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Enter a valid Indian plate (e.g. MH01AB1234)")
    @Size(max = 20)
    private String licensePlate;

    @NotBlank(message = "make is required")
    @Size(min = 1, max = 50, message = "Make must be 1-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\s\\-]{0,48}$", message = "Make contains invalid characters")
    private String make;

    @NotBlank(message = "model is required")
    @Size(min = 1, max = 50, message = "Model must be 1-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\s\\-]{0,48}$", message = "Model contains invalid characters")
    private String model;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    @Pattern(regexp = "^$|^[A-Za-z][A-Za-z\\s\\-]{0,28}[A-Za-z]$", message = "Color must contain only letters")
    private String color;

    @NotBlank(message = "vehicleType is required (2W, 4W, HEAVY)")
    @Pattern(regexp = "^(2W|4W|HEAVY)$", message = "vehicleType must be 2W, 4W, or HEAVY")
    private String vehicleType;

    private Boolean isEV = false;
}
