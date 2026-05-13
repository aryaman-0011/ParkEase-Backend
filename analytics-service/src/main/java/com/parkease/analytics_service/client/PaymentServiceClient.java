package com.parkease.analytics_service.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with payment-service via Eureka service discovery.
 * Replaces hardcoded localhost:8087 RestTemplate calls.
 */
@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @GetMapping("/payments/lot/{lotId}/revenue")
    Map<String, Object> getLotRevenue(@PathVariable("lotId") Long lotId);
}
