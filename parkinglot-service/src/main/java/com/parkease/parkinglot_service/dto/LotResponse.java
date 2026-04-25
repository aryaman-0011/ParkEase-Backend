package com.parkease.parkinglot_service.dto;

import com.parkease.parkinglot_service.entity.ParkingLot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing a parking lot returned to the client.
 * Includes lot details, availability counts, and operational status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotResponse {

    private Long id;
    private Long managerId;
    private String name;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private Double latitude;
    private Double longitude;
    private int totalSpots;
    private int availableSpots;
    private BigDecimal pricePerHour;
    private boolean approved;
    private boolean open;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double distanceKm;

    public static LotResponse from(ParkingLot lot) {
        return LotResponse.builder()
                .id(lot.getId())
                .managerId(lot.getManagerId())
                .name(lot.getName())
                .address(lot.getAddress())
                .city(lot.getCity())
                .state(lot.getState())
                .zipCode(lot.getZipCode())
                .latitude(lot.getLatitude())
                .longitude(lot.getLongitude())
                .totalSpots(lot.getTotalSpots())
                .availableSpots(lot.getAvailableSpots())
                .pricePerHour(lot.getPricePerHour())
                .approved(lot.isApproved())
                .open(lot.isOpen())
                .imageUrl(lot.getImageUrl())
                .description(lot.getDescription())
                .createdAt(lot.getCreatedAt())
                .updatedAt(lot.getUpdatedAt())
                .build();
    }

    public static LotResponse from(ParkingLot lot, double distanceKm) {
        LotResponse response = from(lot);
        response.setDistanceKm(Math.round(distanceKm * 100.0) / 100.0);
        return response;
    }
}
