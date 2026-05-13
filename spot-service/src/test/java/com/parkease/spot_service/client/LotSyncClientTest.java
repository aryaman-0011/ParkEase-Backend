package com.parkease.spot_service.client;

import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.repository.SpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LotSyncClientTest {

    @Mock private ParkingLotServiceClient parkingLotServiceClient;
    @Mock private SpotRepository spotRepository;
    @InjectMocks private LotSyncClient lotSyncClient;

    @Test
    @DisplayName("should sync counts to parkinglot-service")
    void syncSuccess() {
        when(spotRepository.countByLotId(10L)).thenReturn(50L);
        when(spotRepository.countByLotIdAndStatus(10L, SpotStatus.AVAILABLE)).thenReturn(40L);

        lotSyncClient.syncCounts(10L);

        verify(parkingLotServiceClient).syncSpotCounts(10L, 50, 40);
    }

    @Test
    @DisplayName("should not propagate exception on sync failure")
    void syncFailure() {
        when(spotRepository.countByLotId(10L)).thenReturn(50L);
        when(spotRepository.countByLotIdAndStatus(10L, SpotStatus.AVAILABLE)).thenReturn(40L);
        doThrow(new RuntimeException("Connection refused"))
                .when(parkingLotServiceClient).syncSpotCounts(anyLong(), anyInt(), anyInt());

        // Should not throw — non-critical operation
        assertThatCode(() -> lotSyncClient.syncCounts(10L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should sync zero counts for empty lot")
    void syncEmptyLot() {
        when(spotRepository.countByLotId(99L)).thenReturn(0L);
        when(spotRepository.countByLotIdAndStatus(99L, SpotStatus.AVAILABLE)).thenReturn(0L);

        lotSyncClient.syncCounts(99L);

        verify(parkingLotServiceClient).syncSpotCounts(99L, 0, 0);
    }
}
