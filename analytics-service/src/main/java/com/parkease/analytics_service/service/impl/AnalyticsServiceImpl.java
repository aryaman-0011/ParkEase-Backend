package com.parkease.analytics_service.service.impl;

import com.parkease.analytics_service.entity.OccupancyLog;
import com.parkease.analytics_service.repository.AnalyticsRepository;
import com.parkease.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final RestTemplate restTemplate;

    private static final String LOT_ID = "lotId";
    private static final String REVENUE = "revenue";

    @Override
    public void logOccupancy(Long lotId, Long spotId, Double occupancyRate,
                             Integer availableSpots, Integer totalSpots, String vehicleType) {
        OccupancyLog entry = OccupancyLog.builder()
                .lotId(lotId)
                .spotId(spotId)
                .timestamp(LocalDateTime.now())
                .occupancyRate(occupancyRate)
                .availableSpots(availableSpots)
                .totalSpots(totalSpots)
                .vehicleType(vehicleType)
                .build();
        analyticsRepository.save(entry);
        log.info("Occupancy logged: lot={} rate={} available={}/{}", lotId, occupancyRate, availableSpots, totalSpots);
    }

    @Override
    public Double getOccupancyRate(Long lotId) {
        Double avg = analyticsRepository.avgOccupancyByLotId(lotId);
        return avg != null ? Math.round(avg * 10000.0) / 100.0 : 0.0; // percentage with 2 decimals
    }

    @Override
    public Map<Integer, Double> getOccupancyByHour(Long lotId) {
        List<Object[]> rows = analyticsRepository.findPeakHoursByLotId(lotId);
        Map<Integer, Double> hourly = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Integer hour = ((Number) row[0]).intValue();
            Double rate = Math.round(((Number) row[1]).doubleValue() * 10000.0) / 100.0;
            hourly.put(hour, rate);
        }
        return hourly;
    }

    @Override
    public Map<String, Object> getPeakHours(Long lotId) {
        Map<Integer, Double> hourly = getOccupancyByHour(lotId);
        Map<String, Object> result = new LinkedHashMap<>();

        if (hourly.isEmpty()) {
            result.put("peakHour", null);
            result.put("peakRate", 0.0);
            result.put("hourlyBreakdown", hourly);
            return result;
        }

        Map.Entry<Integer, Double> peak = hourly.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        result.put("peakHour", peak != null ? peak.getKey() : null);
        result.put("peakRate", peak != null ? peak.getValue() : 0.0);
        result.put("hourlyBreakdown", hourly);
        return result;
    }

    @Override
    public Map<String, Object> getRevenueByLot(Long lotId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> revenue = restTemplate.getForObject(
                    "http://localhost:8087/payments/lot/{lotId}/revenue", Map.class, lotId);
            result.put(LOT_ID, lotId);
            result.put(REVENUE, revenue);
            log.info("Fetched revenue for lot {}", lotId);
        } catch (Exception e) {
            log.warn("Failed to fetch revenue for lot {}: {}", lotId, e.getMessage());
            result.put(LOT_ID, lotId);
            result.put(REVENUE, null);
            result.put("error", "Revenue data unavailable");
        }
        return result;
    }

    @Override
    public Map<String, Object> getRevenueByDay(Long lotId, LocalDate from, LocalDate to) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(LOT_ID, lotId);
        result.put("from", from.toString());
        result.put("to", to.toString());

        // Query occupancy logs for the date range to derive daily activity
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<OccupancyLog> logs = analyticsRepository.findByLotIdAndTimestampBetween(lotId, start, end);

        Map<String, Long> dailyActivity = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getTimestamp().toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.counting()));
        result.put("dailyActivity", dailyActivity);
        return result;
    }

    @Override
    public Map<String, Long> getMostUsedSpotTypes(Long lotId) {
        List<OccupancyLog> logs = analyticsRepository.findByLotId(lotId);
        return logs.stream()
                .filter(l -> l.getVehicleType() != null)
                .collect(Collectors.groupingBy(
                        OccupancyLog::getVehicleType,
                        Collectors.counting()));
    }

    @Override
    public Double getAvgDuration(Long lotId) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookings = restTemplate.getForObject(
                    "http://localhost:8086/bookings/lot/{lotId}", List.class, lotId);
            if (bookings == null || bookings.isEmpty()) return 0.0;

            double totalMinutes = 0;
            int count = 0;
            for (Map<String, Object> b : bookings) {
                Object start = b.get("startTime");
                Object end = b.get("endTime");
                if (start != null && end != null) {
                    LocalDateTime s = LocalDateTime.parse(start.toString());
                    LocalDateTime e = LocalDateTime.parse(end.toString());
                    totalMinutes += java.time.Duration.between(s, e).toMinutes();
                    count++;
                }
            }
            return count > 0 ? Math.round(totalMinutes / count * 100.0) / 100.0 : 0.0;
        } catch (Exception e) {
            log.warn("Failed to fetch bookings for lot {}: {}", lotId, e.getMessage());
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getPlatformSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<Long> lotIds = analyticsRepository.findDistinctLotIds();

        long totalLogs = analyticsRepository.count();
        Double overallAvg = lotIds.isEmpty() ? 0.0 :
                lotIds.stream()
                        .map(analyticsRepository::avgOccupancyByLotId)
                        .filter(Objects::nonNull)
                        .mapToDouble(d -> d)
                        .average()
                        .orElse(0.0);

        summary.put("totalLotsTracked", lotIds.size());
        summary.put("totalOccupancyLogs", totalLogs);
        summary.put("overallOccupancyRate", Math.round(overallAvg * 10000.0) / 100.0);
        summary.put("lotIds", lotIds);
        log.info("Platform summary: {} lots tracked, {} logs", lotIds.size(), totalLogs);
        return summary;
    }

    @Override
    public Map<String, Object> generateDailyReport(Long lotId) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put(LOT_ID, lotId);
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("occupancyRate", getOccupancyRate(lotId));
        report.put("peakHours", getPeakHours(lotId));
        report.put("spotTypes", getMostUsedSpotTypes(lotId));
        report.put("avgDuration", getAvgDuration(lotId));
        report.put(REVENUE, getRevenueByLot(lotId));

        // Today's activity count
        Long todayCount = analyticsRepository.countTodayByLotId(lotId);
        report.put("todayActivityCount", todayCount != null ? todayCount : 0);

        log.info("Daily report generated for lot {}", lotId);
        return report;
    }
}
