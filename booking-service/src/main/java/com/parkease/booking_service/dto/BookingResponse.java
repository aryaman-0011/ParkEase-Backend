package com.parkease.booking_service.dto;

import com.parkease.booking_service.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Response DTO representing a booking record returned to the client.
 * Includes spot details, time-slot info, vehicle info, and calculated cost.
 */
@Data
@Builder
public class BookingResponse {
    private Long bookingId;
    private Long userId;
    private Long spotId;
    private Long lotId;
    private String lotName;
    private String spotNumber;
    private String vehiclePlate;
    private Long vehicleId;
    private BookingStatus status;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double pricePerHour;
    private Double totalCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
