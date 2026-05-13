package com.parkease.booking_service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Feign client for communicating with spot-service via Eureka service discovery.
 * Replaces RestTemplate calls for spot status changes and data fetching.
 */
@FeignClient(name = "spot-service")
public interface SpotServiceClient {

    @PutMapping("/spots/{spotId}/reserve")
    void reserveSpot(@PathVariable("spotId") Long spotId);

    @PutMapping("/spots/{spotId}/occupy")
    void occupySpot(@PathVariable("spotId") Long spotId);

    @PutMapping("/spots/{spotId}/release")
    void releaseSpot(@PathVariable("spotId") Long spotId);

    @GetMapping("/spots/{spotId}")
    Map<String, Object> getSpotById(@PathVariable("spotId") Long spotId);
}
