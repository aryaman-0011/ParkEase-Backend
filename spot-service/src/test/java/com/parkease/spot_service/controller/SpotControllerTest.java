package com.parkease.spot_service.controller;

import com.parkease.spot_service.dto.*;
import com.parkease.spot_service.enums.*;
import com.parkease.spot_service.service.SpotService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotControllerTest {
    @Mock private SpotService service;
    @InjectMocks private SpotController controller;

    private SpotResponse sample() {
        return SpotResponse.builder().spotId(1L).lotId(2L).spotNumber("A1").floor(1)
                .status(SpotStatus.AVAILABLE).spotType(SpotType.COMPACT).vehicleType(VehicleType.FOUR_WHEELER)
                .pricePerHour(50.0).isEVCharging(false).isHandicapped(false).build();
    }

    @Test void addSpot() {
        when(service.addSpot(any())).thenReturn(sample());
        assertEquals(HttpStatus.CREATED, controller.addSpot(new CreateSpotRequest()).getStatusCode());
    }
    @Test void addBulkSpots() {
        when(service.addBulkSpots(any())).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.CREATED, controller.addBulkSpots(new BulkCreateSpotRequest()).getStatusCode());
    }
    @Test void getSpotById() {
        when(service.getSpotById(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.getSpotById(1L).getStatusCode());
    }
    @Test void getSpotsByLot() {
        when(service.getSpotsByLot(2L)).thenReturn(List.of(sample()));
        assertEquals(1, controller.getSpotsByLot(2L).getBody().size());
    }
    @Test void getAvailableSpots() {
        when(service.getAvailableSpots(2L)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getAvailableSpots(2L).getStatusCode());
    }
    @Test void getSpotsByType() {
        when(service.getSpotsByLotAndType(2L, SpotType.COMPACT)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getSpotsByType(2L, SpotType.COMPACT).getStatusCode());
    }
    @Test void getSpotsByVehicleType() {
        when(service.getSpotsByLotAndVehicleType(2L, VehicleType.FOUR_WHEELER)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getSpotsByVehicleType(2L, VehicleType.FOUR_WHEELER).getStatusCode());
    }
    @Test void getSpotCounts() {
        when(service.getSpotCounts(2L)).thenReturn(SpotCountResponse.builder()
                .lotId(2L).total(10).available(8).reserved(1).occupied(1).build());
        assertEquals(HttpStatus.OK, controller.getSpotCounts(2L).getStatusCode());
    }
    @Test void updateSpot() {
        when(service.updateSpot(eq(1L), any())).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.updateSpot(1L, new UpdateSpotRequest()).getStatusCode());
    }
    @Test void reserveSpot() {
        when(service.reserveSpot(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.reserveSpot(1L).getStatusCode());
    }
    @Test void occupySpot() {
        when(service.occupySpot(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.occupySpot(1L).getStatusCode());
    }
    @Test void releaseSpot() {
        when(service.releaseSpot(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.releaseSpot(1L).getStatusCode());
    }
    @Test void deleteSpot() {
        doNothing().when(service).deleteSpot(1L);
        assertEquals(HttpStatus.OK, controller.deleteSpot(1L).getStatusCode());
    }
    @Test void deleteAllByLot() {
        doNothing().when(service).deleteAllSpotsByLot(2L);
        assertEquals(HttpStatus.OK, controller.deleteAllSpotsByLot(2L).getStatusCode());
    }
}
