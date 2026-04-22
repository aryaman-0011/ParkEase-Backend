package com.parkease.spot_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotCountResponse {
    private Long lotId;
    private long total;
    private long available;
    private long reserved;
    private long occupied;
}
