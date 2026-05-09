package com.parkease.analytics_service.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Declares all occupancy logging, rate computation, peak-hour analysis,
 * revenue aggregation, and report generation operations.
 */
public interface AnalyticsService {

    /** Log a new occupancy snapshot for a parking lot */
    void logOccupancy(Long lotId, Long spotId, Double occupancyRate,
                      Integer availableSpots, Integer totalSpots, String vehicleType);

    /** Get current occupancy rate for a lot (latest average) */
    Double getOccupancyRate(Long lotId);

    /** Get occupancy breakdown by hour-of-day for a lot */
    Map<Integer, Double> getOccupancyByHour(Long lotId);

    /** Get peak hours for a lot (hours with highest average occupancy) */
    Map<String, Object> getPeakHours(Long lotId);

    /** Get revenue for a lot by calling payment-service */
    Map<String, Object> getRevenueByLot(Long lotId);

    /** Get revenue breakdown by day for a lot */
    Map<String, Object> getRevenueByDay(Long lotId, LocalDate from, LocalDate to);

    /** Get most used spot types for a lot */
    Map<String, Long> getMostUsedSpotTypes(Long lotId);

    /** Get average parking duration by calling booking-service */
    Double getAvgDuration(Long lotId);

    /** Get platform-level summary across all lots (admin) */
    Map<String, Object> getPlatformSummary();

    /** Generate daily analytics report for a lot */
    Map<String, Object> generateDailyReport(Long lotId);
}
