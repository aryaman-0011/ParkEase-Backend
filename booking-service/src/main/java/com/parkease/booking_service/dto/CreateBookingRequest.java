package com.parkease.booking_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBookingRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "spotId is required")
    private Long spotId;

    @NotNull(message = "lotId is required")
    private Long lotId;

    private String vehiclePlate;
}
