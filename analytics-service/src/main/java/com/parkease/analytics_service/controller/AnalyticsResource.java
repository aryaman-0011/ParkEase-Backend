package com.parkease.analytics_service.controller;

import com.parkease.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:4200}")
public class AnalyticsResource {

    private static final String LOT_ID = "lotId";
    private final AnalyticsService analyticsService;

    /** POST /analytics/log — Record an occupancy snapshot */
    @PostMapping("/log")
    public ResponseEntity<Void> logOccupancy(@RequestBody Map<String, Object> payload) {
        analyticsService.logOccupancy(
                ((Number) payload.get("lotId")).longValue(),
                ((Number) payload.get("spotId")).longValue(),
                ((Number) payload.get("occupancyRate")).doubleValue(),
                ((Number) payload.get("availableSpots")).intValue(),
                ((Number) payload.get("totalSpots")).intValue(),
                (String) payload.get("vehicleType"));
        return ResponseEntity.ok().build();
    }

    /** GET /analytics/occupancyRate?lotId=1 */
    @GetMapping("/occupancyRate")
    public ResponseEntity<Map<String, Object>> getOccupancyRate(@RequestParam Long lotId) {
        Double rate = analyticsService.getOccupancyRate(lotId);
        return ResponseEntity.ok(Map.of(LOT_ID, lotId, "occupancyRate", rate));
    }

    /** GET /analytics/byHour?lotId=1 */
    @GetMapping("/byHour")
    public ResponseEntity<Map<String, Object>> getOccupancyByHour(@RequestParam Long lotId) {
        return ResponseEntity.ok(Map.of(LOT_ID, lotId, "hourly", analyticsService.getOccupancyByHour(lotId)));
    }

    /** GET /analytics/peakHours?lotId=1 */
    @GetMapping("/peakHours")
    public ResponseEntity<Map<String, Object>> getPeakHours(@RequestParam Long lotId) {
        return ResponseEntity.ok(analyticsService.getPeakHours(lotId));
    }

    /** GET /analytics/revenue?lotId=1 */
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue(@RequestParam Long lotId) {
        return ResponseEntity.ok(analyticsService.getRevenueByLot(lotId));
    }

    /** GET /analytics/revenueByDay?lotId=1&from=2026-05-01&to=2026-05-07 */
    @GetMapping("/revenueByDay")
    public ResponseEntity<Map<String, Object>> getRevenueByDay(
            @RequestParam Long lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getRevenueByDay(lotId, from, to));
    }

    /** GET /analytics/spotTypes?lotId=1 */
    @GetMapping("/spotTypes")
    public ResponseEntity<Map<String, Object>> getSpotTypes(@RequestParam Long lotId) {
        return ResponseEntity.ok(Map.of(LOT_ID, lotId, "spotTypes", analyticsService.getMostUsedSpotTypes(lotId)));
    }

    /** GET /analytics/avgDuration?lotId=1 */
    @GetMapping("/avgDuration")
    public ResponseEntity<Map<String, Object>> getAvgDuration(@RequestParam Long lotId) {
        Double avgMinutes = analyticsService.getAvgDuration(lotId);
        return ResponseEntity.ok(Map.of(LOT_ID, lotId, "avgDurationMinutes", avgMinutes));
    }

    /** GET /analytics/platformSummary */
    @GetMapping("/platformSummary")
    public ResponseEntity<Map<String, Object>> getPlatformSummary() {
        return ResponseEntity.ok(analyticsService.getPlatformSummary());
    }

    /** GET /analytics/dailyReport?lotId=1 */
    @GetMapping("/dailyReport")
    public ResponseEntity<Map<String, Object>> getDailyReport(@RequestParam Long lotId) {
        return ResponseEntity.ok(analyticsService.generateDailyReport(lotId));
    }
}
