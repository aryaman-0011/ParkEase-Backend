package com.parkease.vehicle_service.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {
    @Test void builderAndGetters() {
        var v = Vehicle.builder().vehicleId(1L).ownerId(10L).licensePlate("KA01AB1234")
                .make("Toyota").model("Camry").color("White").vehicleType("FOUR_WHEELER")
                .isEV(false).isActive(true).build();
        assertEquals(1L, v.getVehicleId());
        assertEquals("KA01AB1234", v.getLicensePlate());
        assertFalse(v.getIsEV());
        assertTrue(v.getIsActive());
    }
    @Test void prePersist() {
        var v = new Vehicle();
        v.onCreate();
        assertNotNull(v.getCreatedAt());
        assertNotNull(v.getRegisteredAt());
    }
    @Test void preUpdate() {
        var v = new Vehicle();
        v.onUpdate();
        assertNotNull(v.getUpdatedAt());
    }
    @Test void setters() {
        var v = new Vehicle();
        v.setVehicleId(2L);
        v.setLicensePlate("MH01");
        assertEquals(2L, v.getVehicleId());
    }
}
