package com.parkease.booking_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Size(max = 20)
    @Pattern(regexp = "^$|^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Enter a valid Indian vehicle plate")
    private String vehiclePlate;

    @NotNull(message = "scheduledStartTime is required")
    @FutureOrPresent(message = "Start time cannot be in the past")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "scheduledEndTime is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime scheduledEndTime;
}
