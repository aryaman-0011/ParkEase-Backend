package com.parkease.payment_service.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with booking-service via Eureka service discovery.
 * Replaces RestTemplate calls for fetching lot bookings for revenue calculation.
 */
@FeignClient(name = "booking-service")
public interface BookingServiceClient {

    @GetMapping("/bookings/lot/{lotId}")
    List<Map<String, Object>> getBookingsByLot(@PathVariable("lotId") Long lotId);
}
