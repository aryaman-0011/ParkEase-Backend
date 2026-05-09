package com.parkease.parkinglot_service.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ParkingLotTest {
    @Test void builderAndGetters() {
        var lot = ParkingLot.builder().id(1L).managerId(10L).name("Test").address("MG Road")
                .city("Bangalore").state("KA").zipCode("560001").latitude(12.9).longitude(77.6)
                .totalSpots(50).availableSpots(40).pricePerHour(new BigDecimal("30.00"))
                .approved(true).open(true).build();
        assertEquals(1L, lot.getId());
        assertEquals("Bangalore", lot.getCity());
        assertTrue(lot.isApproved());
        assertTrue(lot.isOpen());
    }
    @Test void prePersist() {
        var lot = new ParkingLot();
        lot.onCreate();
        assertNotNull(lot.getCreatedAt());
    }
    @Test void preUpdate() {
        var lot = new ParkingLot();
        lot.onUpdate();
        assertNotNull(lot.getUpdatedAt());
    }
    @Test void setters() {
        var lot = new ParkingLot();
        lot.setName("Updated");
        lot.setApproved(false);
        lot.setOpen(false);
        assertEquals("Updated", lot.getName());
        assertFalse(lot.isApproved());
    }
}
