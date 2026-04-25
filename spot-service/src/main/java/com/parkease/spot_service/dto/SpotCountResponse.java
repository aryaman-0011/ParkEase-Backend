package com.parkease.spot_service.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO with aggregated spot counts by status.
 * Returns total, available, reserved, and occupied counts for a lot.
 */
@Data
@Builder
public class SpotCountResponse {
    private Long lotId;
    private long total;
    private long available;
    private long reserved;
    private long occupied;
}
