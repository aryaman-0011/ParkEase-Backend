package com.parkease.spot_service.dto;

import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import lombok.Data;

/**
 * Request DTO for updating an existing parking spot.
 */
@Data
public class UpdateSpotRequest {
    private String spotNumber;
    private Integer floor;
    private SpotType spotType;
    private VehicleType vehicleType;
    private Boolean isHandicapped;
    private Boolean isEVCharging;
    private Double pricePerHour;
}
