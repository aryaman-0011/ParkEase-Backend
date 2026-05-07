package com.parkease.spot_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response DTO with aggregated spot counts by status.
 * Returns total, available, reserved, and occupied counts for a lot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotCountResponse implements Serializable {
    private Long lotId;
    private long total;
    private long available;
    private long reserved;
    private long occupied;
}
