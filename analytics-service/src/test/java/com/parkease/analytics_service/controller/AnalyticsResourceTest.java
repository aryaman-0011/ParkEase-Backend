package com.parkease.analytics_service.controller;

import com.parkease.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsResourceTest {
    @Mock private AnalyticsService service;
    @InjectMocks private AnalyticsResource controller;

    @Test void logOccupancy() {
        doNothing().when(service).logOccupancy(anyLong(), anyLong(), anyDouble(), anyInt(), anyInt(), anyString());
        Map<String, Object> p = new HashMap<>();
        p.put("lotId", 1); p.put("spotId", 5); p.put("occupancyRate", 75.0);
        p.put("availableSpots", 10); p.put("totalSpots", 40); p.put("vehicleType", "FOUR_WHEELER");
        assertEquals(HttpStatus.OK, controller.logOccupancy(p).getStatusCode());
    }
    @Test void getOccupancyRate() {
        when(service.getOccupancyRate(1L)).thenReturn(75.0);
        var r = controller.getOccupancyRate(1L);
        assertEquals(75.0, r.getBody().get("occupancyRate"));
    }
    @Test void getByHour() {
        when(service.getOccupancyByHour(1L)).thenReturn(Map.of(9, 60.0));
        assertEquals(HttpStatus.OK, controller.getOccupancyByHour(1L).getStatusCode());
    }
    @Test void getPeakHours() {
        when(service.getPeakHours(1L)).thenReturn(Map.of("peakHour", 14));
        assertEquals(HttpStatus.OK, controller.getPeakHours(1L).getStatusCode());
    }
    @Test void getRevenue() {
        when(service.getRevenueByLot(1L)).thenReturn(Map.of("revenue", 5000));
        assertEquals(HttpStatus.OK, controller.getRevenue(1L).getStatusCode());
    }
    @Test void getRevenueByDay() {
        when(service.getRevenueByDay(eq(1L), any(), any())).thenReturn(Map.of("data", List.of()));
        assertEquals(HttpStatus.OK, controller.getRevenueByDay(1L, LocalDate.now(), LocalDate.now()).getStatusCode());
    }
    @Test void getSpotTypes() {
        when(service.getMostUsedSpotTypes(1L)).thenReturn(Map.of("COMPACT", 10L));
        assertEquals(HttpStatus.OK, controller.getSpotTypes(1L).getStatusCode());
    }
    @Test void getAvgDuration() {
        when(service.getAvgDuration(1L)).thenReturn(45.0);
        assertEquals(45.0, controller.getAvgDuration(1L).getBody().get("avgDurationMinutes"));
    }
    @Test void getPlatformSummary() {
        when(service.getPlatformSummary()).thenReturn(Map.of("totalLots", 5));
        assertEquals(HttpStatus.OK, controller.getPlatformSummary().getStatusCode());
    }
    @Test void getDailyReport() {
        when(service.generateDailyReport(1L)).thenReturn(Map.of("rate", 70));
        assertEquals(HttpStatus.OK, controller.getDailyReport(1L).getStatusCode());
    }
}
