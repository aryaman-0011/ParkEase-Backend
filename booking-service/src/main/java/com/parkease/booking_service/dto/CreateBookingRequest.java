package com.parkease.booking_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a new time-slot booking.
 * Contains user, spot, vehicle, and scheduled start/end time information.
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "spotId is required")
    private Long spotId;

    @NotNull(message = "lotId is required")
    private Long lotId;

    private Long vehicleId;

    private String vehiclePlate;

    @NotNull(message = "scheduledStartTime is required")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "scheduledEndTime is required")
    private LocalDateTime scheduledEndTime;
}
