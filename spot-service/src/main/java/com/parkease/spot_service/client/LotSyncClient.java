package com.parkease.spot_service.client;

import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls parkinglot-service via Eureka to keep totalSpots / availableSpots in sync.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LotSyncClient {

    private final RestTemplate restTemplate;
    private final SpotRepository spotRepository;

    private static final String LOT_SERVICE_URL = "http://parkinglot-service/lots";

    /**
     * Reads current counts from spot DB and pushes them to parkinglot-service.
     */
    public void syncCounts(Long lotId) {
        try {
            long total = spotRepository.countByLotId(lotId);
            long available = spotRepository.countByLotIdAndStatus(lotId, SpotStatus.AVAILABLE);

            String url = LOT_SERVICE_URL + "/" + lotId + "/sync-spots?total=" + total + "&available=" + available;
            restTemplate.put(url, null);
            log.info("Synced lot {} counts: total={}, available={}", lotId, total, available);
        } catch (Exception e) {
            // Non-critical — log and continue. The lot page will still work.
            log.warn("Failed to sync spot counts for lot {}: {}", lotId, e.getMessage());
        }
    }
}
