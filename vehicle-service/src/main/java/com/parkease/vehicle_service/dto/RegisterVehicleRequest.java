package com.parkease.vehicle_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterVehicleRequest {

    @NotNull(message = "ownerId is required")
    private Long ownerId;

    @NotBlank(message = "licensePlate is required")
    private String licensePlate;

    @NotBlank(message = "make is required")
    private String make;

    @NotBlank(message = "model is required")
    private String model;

    private String color;

    @NotBlank(message = "vehicleType is required (2W, 4W, HEAVY)")
    private String vehicleType;

    private Boolean isEV = false;
}
