package com.parkease.spot_service.dto;

import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SpotResponse {

    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private Integer floor;
    private SpotType spotType;
    private VehicleType vehicleType;
    private SpotStatus status;
    private Boolean isHandicapped;
    private Boolean isEVCharging;
    private Double pricePerHour;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SpotResponse from(ParkingSpot spot) {
        return SpotResponse.builder()
                .spotId(spot.getSpotId())
                .lotId(spot.getLotId())
                .spotNumber(spot.getSpotNumber())
                .floor(spot.getFloor())
                .spotType(spot.getSpotType())
                .vehicleType(spot.getVehicleType())
                .status(spot.getStatus())
                .isHandicapped(spot.getIsHandicapped())
                .isEVCharging(spot.getIsEVCharging())
                .pricePerHour(spot.getPricePerHour())
                .createdAt(spot.getCreatedAt())
                .updatedAt(spot.getUpdatedAt())
                .build();
    }
}
