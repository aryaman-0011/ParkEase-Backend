package com.parkease.spot_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for communicating with parkinglot-service via Eureka service discovery.
 * Replaces RestTemplate calls for syncing spot counts with the lot.
 */
@FeignClient(name = "parkinglot-service")
public interface ParkingLotServiceClient {

    @PutMapping("/lots/{lotId}/sync-spots")
    void syncSpotCounts(@PathVariable("lotId") Long lotId,
                        @RequestParam("total") int total,
                        @RequestParam("available") int available);
}
