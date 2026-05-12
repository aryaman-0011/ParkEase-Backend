package com.parkease.analytics_service.service.impl;

import com.parkease.analytics_service.entity.OccupancyLog;
import com.parkease.analytics_service.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private RestTemplate restTemplate;
    @InjectMocks private AnalyticsServiceImpl analyticsService;

    private OccupancyLog log1;
    private OccupancyLog log2;

    @BeforeEach
    void setUp() {
        log1 = OccupancyLog.builder()
                .logId(1L).lotId(10L).spotId(100L)
                .timestamp(LocalDateTime.now().minusHours(2))
                .occupancyRate(0.75).availableSpots(5).totalSpots(20)
                .vehicleType("FOUR_WHEELER").build();
        log2 = OccupancyLog.builder()
                .logId(2L).lotId(10L).spotId(101L)
                .timestamp(LocalDateTime.now().minusHours(1))
                .occupancyRate(0.85).availableSpots(3).totalSpots(20)
                .vehicleType("TWO_WHEELER").build();
    }

    @Nested
    @DisplayName("logOccupancy")
    class LogOccupancy {
        @Test
        @DisplayName("should save occupancy log entry")
        void logSuccess() {
            when(analyticsRepository.save(any(OccupancyLog.class))).thenAnswer(i -> i.getArgument(0));

            analyticsService.logOccupancy(10L, 100L, 0.75, 5, 20, "FOUR_WHEELER");

            verify(analyticsRepository).save(any(OccupancyLog.class));
        }
    }

    @Nested
    @DisplayName("getOccupancyRate")
    class GetOccupancyRate {
        @Test
        @DisplayName("should return average occupancy as percentage")
        void withData() {
            when(analyticsRepository.avgOccupancyByLotId(10L)).thenReturn(0.78);

            Double rate = analyticsService.getOccupancyRate(10L);

            assertThat(rate).isEqualTo(78.0);
        }

        @Test
        @DisplayName("should return 0 when no data")
        void noData() {
            when(analyticsRepository.avgOccupancyByLotId(99L)).thenReturn(null);

            assertThat(analyticsService.getOccupancyRate(99L)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getOccupancyByHour")
    class GetByHour {
        @Test
        @DisplayName("should return hourly breakdown")
        void hourlyData() {
            List<Object[]> rows = Arrays.asList(
                    new Object[]{8, 0.65},
                    new Object[]{12, 0.92},
                    new Object[]{18, 0.88}
            );
            doReturn(rows).when(analyticsRepository).findPeakHoursByLotId(10L);

            Map<Integer, Double> result = analyticsService.getOccupancyByHour(10L);

            assertThat(result).hasSize(3);
            assertThat(result).containsEntry(12, 92.0);
        }
    }

    @Nested
    @DisplayName("getPeakHours")
    class GetPeakHours {
        @Test
        @DisplayName("should identify peak hour")
        void peakFound() {
            List<Object[]> rows = Arrays.asList(
                    new Object[]{9, 0.60},
                    new Object[]{17, 0.95}
            );
            doReturn(rows).when(analyticsRepository).findPeakHoursByLotId(10L);

            Map<String, Object> result = analyticsService.getPeakHours(10L);

            assertThat(result).containsEntry("peakHour", 17);
            assertThat(result).containsEntry("peakRate", 95.0);
        }

        @Test
        @DisplayName("should handle no data gracefully")
        void noPeak() {
            when(analyticsRepository.findPeakHoursByLotId(99L)).thenReturn(List.of());

            Map<String, Object> result = analyticsService.getPeakHours(99L);

            assertThat(result.get("peakHour")).isNull();
        }
    }

    @Nested
    @DisplayName("getMostUsedSpotTypes")
    class SpotTypes {
        @Test
        @DisplayName("should aggregate vehicle types")
        void spotTypeCounts() {
            OccupancyLog log3 = OccupancyLog.builder()
                    .logId(3L).lotId(10L).spotId(102L)
                    .timestamp(LocalDateTime.now())
                    .occupancyRate(0.5).availableSpots(10).totalSpots(20)
                    .vehicleType("FOUR_WHEELER").build();

            when(analyticsRepository.findByLotId(10L)).thenReturn(List.of(log1, log2, log3));

            Map<String, Long> types = analyticsService.getMostUsedSpotTypes(10L);

            assertThat(types).containsEntry("FOUR_WHEELER", 2L);
            assertThat(types).containsEntry("TWO_WHEELER", 1L);
        }
    }

    @Nested
    @DisplayName("getRevenueByLot")
    class Revenue {
        @Test
        @DisplayName("should fetch revenue from payment-service")
        void revenueSuccess() {
            Map<String, Object> revenueData = Map.of("totalRevenue", 5000.0);
            when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L)))
                    .thenReturn(revenueData);

            Map<String, Object> result = analyticsService.getRevenueByLot(10L);

            assertThat(result).containsEntry("lotId", 10L);
            assertThat(result.get("revenue")).isNotNull();
        }

        @Test
        @DisplayName("should handle payment-service failure gracefully")
        void revenueFail() {
            when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L)))
                    .thenThrow(new RuntimeException("Connection refused"));

            Map<String, Object> result = analyticsService.getRevenueByLot(10L);

            assertThat(result).containsEntry("error", "Revenue data unavailable");
        }
    }

    @Nested
    @DisplayName("getPlatformSummary")
    class PlatformSummary {
        @Test
        @DisplayName("should aggregate across all lots")
        void summary() {
            when(analyticsRepository.findDistinctLotIds()).thenReturn(List.of(10L, 20L));
            when(analyticsRepository.count()).thenReturn(100L);
            when(analyticsRepository.avgOccupancyByLotId(10L)).thenReturn(0.7);
            when(analyticsRepository.avgOccupancyByLotId(20L)).thenReturn(0.8);

            Map<String, Object> result = analyticsService.getPlatformSummary();

            assertThat(result).containsEntry("totalLotsTracked", 2);
            assertThat(result).containsEntry("totalOccupancyLogs", 100L);
            assertThat((Double) result.get("overallOccupancyRate")).isPositive();
        }

        @Test
        @DisplayName("should handle empty platform")
        void emptyPlatform() {
            when(analyticsRepository.findDistinctLotIds()).thenReturn(List.of());
            when(analyticsRepository.count()).thenReturn(0L);

            Map<String, Object> result = analyticsService.getPlatformSummary();

            assertThat(result).containsEntry("totalLotsTracked", 0);
        }
    }

    @Nested
    @DisplayName("generateDailyReport")
    class DailyReport {
        @Test
        @DisplayName("should compile full daily report")
        void report() {
            when(analyticsRepository.avgOccupancyByLotId(10L)).thenReturn(0.72);
            List<Object[]> peakRows = new java.util.ArrayList<>();
            peakRows.add(new Object[]{14, 0.9});
            doReturn(peakRows).when(analyticsRepository).findPeakHoursByLotId(10L);
            when(analyticsRepository.findByLotId(10L)).thenReturn(List.of(log1, log2));
            when(analyticsRepository.countTodayByLotId(10L)).thenReturn(5L);
            when(restTemplate.getForObject(contains("payments"), eq(Map.class), eq(10L)))
                    .thenReturn(Map.of("totalRevenue", 3000.0));
            when(restTemplate.getForObject(contains("bookings"), eq(List.class), eq(10L)))
                    .thenReturn(null);

            Map<String, Object> report = analyticsService.generateDailyReport(10L);

            assertThat(report).containsEntry("lotId", 10L);
            assertThat(report.get("occupancyRate")).isNotNull();
            assertThat(report.get("peakHours")).isNotNull();
            assertThat(report).containsEntry("todayActivityCount", 5L);
        }
    }
}
