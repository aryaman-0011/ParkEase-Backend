package com.parkease.spot_service.dto;

import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a single parking spot.
 * Contains spot number, type, floor, vehicle type, and pricing.
 */
@Data
public class CreateSpotRequest {

    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotBlank(message = "Spot number is required")
    private String spotNumber;

    @Min(value = 0, message = "Floor must be >= 0")
    private Integer floor = 0;

    @NotNull(message = "Spot type is required")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private Boolean isHandicapped = false;
    private Boolean isEVCharging = false;
    private Double pricePerHour;
}
