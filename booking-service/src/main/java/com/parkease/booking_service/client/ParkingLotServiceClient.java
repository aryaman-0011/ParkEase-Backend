package com.parkease.booking_service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with parkinglot-service via Eureka service discovery.
 * Replaces RestTemplate calls for fetching lot details (pricing, names).
 */
@FeignClient(name = "parkinglot-service")
public interface ParkingLotServiceClient {

    @GetMapping("/lots/{lotId}")
    Map<String, Object> getLotById(@PathVariable("lotId") Long lotId);
}
