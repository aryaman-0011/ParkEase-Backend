package com.parkease.vehicle_service.controller;

import com.parkease.vehicle_service.dto.*;
import com.parkease.vehicle_service.service.VehicleService;
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
class VehicleResourceTest {
    @Mock private VehicleService service;
    @InjectMocks private VehicleResource controller;

    private VehicleResponse sample() {
        return VehicleResponse.builder().vehicleId(1L).ownerId(10L).licensePlate("KA01")
                .make("Toyota").model("Camry").vehicleType("FOUR_WHEELER").isEV(false).build();
    }

    @Test void register() {
        when(service.registerVehicle(any())).thenReturn(sample());
        assertEquals(HttpStatus.CREATED, controller.registerVehicle(new RegisterVehicleRequest()).getStatusCode());
    }
    @Test void getById() {
        when(service.getVehicleById(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.getById(1L).getStatusCode());
    }
    @Test void getByOwner() {
        when(service.getVehiclesByOwner(10L)).thenReturn(List.of(sample()));
        assertEquals(1, controller.getByOwner(10L).getBody().size());
    }
    @Test void getByPlateFound() {
        when(service.getByLicensePlate("KA01")).thenReturn(Optional.of(sample()));
        assertEquals(HttpStatus.OK, controller.getByPlate("KA01").getStatusCode());
    }
    @Test void getByPlateNotFound() {
        when(service.getByLicensePlate("X")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.getByPlate("X").getStatusCode());
    }
    @Test void update() {
        when(service.updateVehicle(eq(1L), any())).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.updateVehicle(1L, new UpdateVehicleRequest()).getStatusCode());
    }
    @Test void deleteVehicle() {
        doNothing().when(service).deleteVehicle(1L);
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteVehicle(1L).getStatusCode());
    }
    @Test void getType() {
        when(service.getVehicleType(1L)).thenReturn("FOUR_WHEELER");
        assertEquals("FOUR_WHEELER", controller.getType(1L).getBody().get("vehicleType"));
    }
    @Test void isEV() {
        when(service.isEVVehicle(1L)).thenReturn(false);
        assertFalse(controller.isEV(1L).getBody().get("isEV"));
    }
    @Test void getAll() {
        when(service.getAllVehicles()).thenReturn(List.of(sample()));
        assertEquals(1, controller.getAll().getBody().size());
    }
}
