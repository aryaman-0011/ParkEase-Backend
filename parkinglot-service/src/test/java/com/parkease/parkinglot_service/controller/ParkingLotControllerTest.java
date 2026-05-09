package com.parkease.parkinglot_service.controller;

import com.parkease.parkinglot_service.dto.*;
import com.parkease.parkinglot_service.service.ParkingLotService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotControllerTest {
    @Mock private ParkingLotService service;
    @InjectMocks private ParkingLotController controller;

    private LotResponse sample() {
        return LotResponse.builder().id(1L).name("Test Lot").city("Bangalore")
                .address("MG Road").managerId(10L).approved(true).open(true)
                .totalSpots(50).availableSpots(40).pricePerHour(new BigDecimal("30.00")).build();
    }

    @Test void createLot() {
        when(service.createLot(eq(10L), any())).thenReturn(sample());
        assertEquals(HttpStatus.CREATED, controller.createLot(10L, new CreateLotRequest()).getStatusCode());
    }
    @Test void updateLot() {
        when(service.updateLot(eq(10L), eq(1L), any())).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.updateLot(10L, 1L, new UpdateLotRequest()).getStatusCode());
    }
    @Test void toggleOpenClose() {
        when(service.toggleOpenClose(10L, 1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.toggleOpenClose(10L, 1L).getStatusCode());
    }
    @Test void getMyLots() {
        when(service.getMyLots(10L)).thenReturn(List.of(sample()));
        assertEquals(1, controller.getMyLots(10L).getBody().size());
    }
    @Test void deleteLot() {
        doNothing().when(service).deleteLot(10L, 1L);
        assertEquals(HttpStatus.OK, controller.deleteLot(10L, 1L).getStatusCode());
    }
    @Test void searchByCity() {
        when(service.searchByCity("Bangalore")).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.searchByCity("Bangalore").getStatusCode());
    }
    @Test void findNearbyLots() {
        when(service.findNearbyLots(12.9, 77.6, 10.0)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.findNearbyLots(12.9, 77.6, 10.0).getStatusCode());
    }
    @Test void getLotById() {
        when(service.getLotById(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.getLotById(1L).getStatusCode());
    }
    @Test void getAllApprovedLots() {
        when(service.getAllApprovedLots()).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getAllApprovedLots().getStatusCode());
    }
    @Test void getPendingLots() {
        when(service.getPendingLots()).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getPendingLots().getStatusCode());
    }
    @Test void approveLot() {
        when(service.approveLot(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.approveLot(1L).getStatusCode());
    }
    @Test void rejectLot() {
        when(service.rejectLot(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.rejectLot(1L).getStatusCode());
    }
    @Test void getAllLots() {
        when(service.getAllLots()).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getAllLots().getStatusCode());
    }
    @Test void deleteLotAsAdmin() {
        doNothing().when(service).deleteLotAsAdmin(1L);
        assertEquals(HttpStatus.OK, controller.deleteLotAsAdmin(1L).getStatusCode());
    }
    @Test void getLotsByManagerId() {
        when(service.getLotsByManagerId(10L)).thenReturn(List.of(sample()));
        assertEquals(HttpStatus.OK, controller.getLotsByManagerId(10L).getStatusCode());
    }
    @Test void decrementSpotSuccess() {
        when(service.decrementAvailableSpots(1L)).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.decrementSpot(1L).getStatusCode());
    }
    @Test void decrementSpotConflict() {
        when(service.decrementAvailableSpots(1L)).thenReturn(false);
        assertEquals(HttpStatus.CONFLICT, controller.decrementSpot(1L).getStatusCode());
    }
    @Test void incrementSpotSuccess() {
        when(service.incrementAvailableSpots(1L)).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.incrementSpot(1L).getStatusCode());
    }
    @Test void incrementSpotConflict() {
        when(service.incrementAvailableSpots(1L)).thenReturn(false);
        assertEquals(HttpStatus.CONFLICT, controller.incrementSpot(1L).getStatusCode());
    }
    @Test void syncSpotCounts() {
        doNothing().when(service).syncSpotCounts(1L, 50, 40);
        assertEquals(HttpStatus.OK, controller.syncSpotCounts(1L, 50, 40).getStatusCode());
    }
    @Test void deleteAllByManager() {
        doNothing().when(service).deleteAllLotsByManager(10L);
        assertEquals(HttpStatus.OK, controller.deleteAllLotsByManager(10L).getStatusCode());
    }
}
