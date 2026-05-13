package com.parkease.parkinglot_service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for communicating with auth-service via Eureka service discovery.
 * Replaces hardcoded localhost:8081 inline RestTemplate calls.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/auth/internal/admin-ids")
    Map<String, Object> getAdminIds();
}
