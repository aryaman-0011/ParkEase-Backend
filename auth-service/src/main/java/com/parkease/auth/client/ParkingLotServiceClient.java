package com.parkease.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with parkinglot-service via Eureka service discovery.
 * Replaces hardcoded localhost:8084 inline RestClient calls.
 */
@FeignClient(name = "parkinglot-service")
public interface ParkingLotServiceClient {

    @DeleteMapping("/lots/manager/{managerId}")
    void deleteAllLotsByManager(@PathVariable("managerId") Long managerId);
}
