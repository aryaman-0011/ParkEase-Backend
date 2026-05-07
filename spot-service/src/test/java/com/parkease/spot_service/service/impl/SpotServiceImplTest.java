package com.parkease.spot_service.service.impl;

import com.parkease.spot_service.client.LotSyncClient;
import com.parkease.spot_service.dto.*;
import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.exception.BadRequestException;
import com.parkease.spot_service.exception.ResourceNotFoundException;
import com.parkease.spot_service.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotServiceImplTest {

    @Mock private SpotRepository spotRepository;
    @Mock private LotSyncClient lotSyncClient;
    @InjectMocks private SpotServiceImpl spotService;

    private ParkingSpot spot;

    @BeforeEach
    void setUp() {
        spot = ParkingSpot.builder()
                .spotId(1L).lotId(10L).spotNumber("A-01").floor(1)
                .spotType(SpotType.STANDARD).vehicleType(VehicleType.FOUR_WHEELER)
                .isHandicapped(false).isEVCharging(false)
                .pricePerHour(50.0).status(SpotStatus.AVAILABLE)
                .build();
    }

    private CreateSpotRequest buildCreateRequest() {
        CreateSpotRequest req = new CreateSpotRequest();
        req.setLotId(10L);
        req.setSpotNumber("A-01");
        req.setFloor(1);
        req.setSpotType(SpotType.STANDARD);
        req.setVehicleType(VehicleType.FOUR_WHEELER);
        req.setIsHandicapped(false);
        req.setIsEVCharging(false);
        req.setPricePerHour(50.0);
        return req;
    }

    private BulkCreateSpotRequest buildBulkRequest() {
        BulkCreateSpotRequest req = new BulkCreateSpotRequest();
        req.setLotId(10L);
        req.setCount(3);
        req.setPrefix("B");
        req.setStartFrom(1);
        req.setFloor(2);
        req.setSpotType(SpotType.STANDARD);
        req.setVehicleType(VehicleType.FOUR_WHEELER);
        req.setIsHandicapped(false);
        req.setIsEVCharging(false);
        req.setPricePerHour(40.0);
        return req;
    }

    @Nested
    @DisplayName("addSpot")
    class AddSpot {
        @Test
        @DisplayName("should create spot and sync lot counts")
        void addSuccess() {
            when(spotRepository.save(any(ParkingSpot.class))).thenReturn(spot);

            SpotResponse response = spotService.addSpot(buildCreateRequest());

            assertThat(response.getSpotNumber()).isEqualTo("A-01");
            assertThat(response.getStatus()).isEqualTo(SpotStatus.AVAILABLE);
            verify(lotSyncClient).syncCounts(10L);
        }
    }

    @Nested
    @DisplayName("addBulkSpots")
    class AddBulkSpots {
        @Test
        @DisplayName("should create numbered spots with prefix")
        void bulkAdd() {
            when(spotRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<ParkingSpot> list = inv.getArgument(0);
                for (int i = 0; i < list.size(); i++) list.get(i).setSpotId((long) (i + 1));
                return list;
            });

            List<SpotResponse> result = spotService.addBulkSpots(buildBulkRequest());

            assertThat(result).hasSize(3);
            verify(lotSyncClient).syncCounts(10L);
        }
    }

    @Nested
    @DisplayName("getSpotById")
    class GetSpotById {
        @Test
        @DisplayName("should return spot when found")
        void found() {
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));

            SpotResponse response = spotService.getSpotById(1L);

            assertThat(response.getSpotNumber()).isEqualTo("A-01");
        }

        @Test
        @DisplayName("should throw when not found")
        void notFound() {
            when(spotRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> spotService.getSpotById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateSpot")
    class UpdateSpot {
        @Test
        @DisplayName("should update only provided fields")
        void updatePartial() {
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));
            when(spotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UpdateSpotRequest req = new UpdateSpotRequest();
            req.setPricePerHour(75.0);
            req.setIsEVCharging(true);

            SpotResponse response = spotService.updateSpot(1L, req);

            assertThat(response.getPricePerHour()).isEqualTo(75.0);
            assertThat(response.getIsEVCharging()).isTrue();
            assertThat(response.getSpotNumber()).isEqualTo("A-01"); // unchanged
        }
    }

    @Nested
    @DisplayName("deleteSpot")
    class DeleteSpot {
        @Test
        @DisplayName("should delete and sync counts")
        void deleteSuccess() {
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));

            spotService.deleteSpot(1L);

            verify(spotRepository).deleteById(1L);
            verify(lotSyncClient).syncCounts(10L);
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {
        @Test
        @DisplayName("getSpotsByLot should return ordered spots")
        void spotsByLot() {
            when(spotRepository.findByLotIdOrderByFloorAscSpotNumberAsc(10L))
                    .thenReturn(List.of(spot));

            assertThat(spotService.getSpotsByLot(10L)).hasSize(1);
        }

        @Test
        @DisplayName("getAvailableSpots should filter by AVAILABLE")
        void availableSpots() {
            when(spotRepository.findByLotIdAndStatus(10L, SpotStatus.AVAILABLE))
                    .thenReturn(List.of(spot));

            assertThat(spotService.getAvailableSpots(10L)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSpotCounts")
    class GetSpotCounts {
        @Test
        @DisplayName("should aggregate counts correctly")
        void counts() {
            when(spotRepository.countByLotId(10L)).thenReturn(20L);
            when(spotRepository.countByLotIdAndStatus(10L, SpotStatus.AVAILABLE)).thenReturn(15L);
            when(spotRepository.countByLotIdAndStatus(10L, SpotStatus.RESERVED)).thenReturn(3L);
            when(spotRepository.countByLotIdAndStatus(10L, SpotStatus.OCCUPIED)).thenReturn(2L);

            SpotCountResponse counts = spotService.getSpotCounts(10L);

            assertThat(counts.getTotal()).isEqualTo(20);
            assertThat(counts.getAvailable()).isEqualTo(15);
            assertThat(counts.getReserved()).isEqualTo(3);
            assertThat(counts.getOccupied()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {
        @Test
        @DisplayName("reserveSpot should change AVAILABLE -> RESERVED")
        void reserve() {
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));
            when(spotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SpotResponse response = spotService.reserveSpot(1L);

            assertThat(response.getStatus()).isEqualTo(SpotStatus.RESERVED);
            verify(lotSyncClient).syncCounts(10L);
        }

        @Test
        @DisplayName("reserveSpot should throw if not AVAILABLE")
        void reserveNotAvailable() {
            spot.setStatus(SpotStatus.OCCUPIED);
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));

            assertThatThrownBy(() -> spotService.reserveSpot(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("occupySpot should change RESERVED -> OCCUPIED")
        void occupy() {
            spot.setStatus(SpotStatus.RESERVED);
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));
            when(spotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SpotResponse response = spotService.occupySpot(1L);

            assertThat(response.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
        }

        @Test
        @DisplayName("occupySpot should throw if not RESERVED")
        void occupyNotReserved() {
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));

            assertThatThrownBy(() -> spotService.occupySpot(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("RESERVED");
        }

        @Test
        @DisplayName("releaseSpot should change back to AVAILABLE")
        void release() {
            spot.setStatus(SpotStatus.OCCUPIED);
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));
            when(spotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SpotResponse response = spotService.releaseSpot(1L);

            assertThat(response.getStatus()).isEqualTo(SpotStatus.AVAILABLE);
        }

        @Test
        @DisplayName("releaseSpot should throw if already AVAILABLE")
        void releaseAlreadyAvailable() {
            when(spotRepository.findById(1L)).thenReturn(Optional.of(spot));

            assertThatThrownBy(() -> spotService.releaseSpot(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already available");
        }
    }

    @Nested
    @DisplayName("deleteAllSpotsByLot")
    class DeleteAllByLot {
        @Test
        @DisplayName("should bulk delete and sync")
        void bulkDelete() {
            spotService.deleteAllSpotsByLot(10L);

            verify(spotRepository).deleteAllByLotId(10L);
            verify(lotSyncClient).syncCounts(10L);
        }
    }
}
