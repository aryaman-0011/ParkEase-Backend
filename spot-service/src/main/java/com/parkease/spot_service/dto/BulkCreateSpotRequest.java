package com.parkease.spot_service.dto;

import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkCreateSpotRequest {

    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotNull(message = "Spot type is required")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @Min(value = 0, message = "Floor must be >= 0")
    private Integer floor = 0;

    @Min(value = 1, message = "Count must be at least 1")
    @NotNull(message = "Count is required")
    private Integer count;

    /** Prefix for auto-numbering, e.g. "A" → A-01, A-02 ... */
    private String prefix = "";

    /** Starting number for auto-generated spot numbers */
    private Integer startFrom = 1;

    private Boolean isHandicapped = false;
    private Boolean isEVCharging = false;
    private Double pricePerHour;
}
