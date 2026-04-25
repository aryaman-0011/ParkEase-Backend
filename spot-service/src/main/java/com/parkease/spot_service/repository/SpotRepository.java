package com.parkease.spot_service.repository;

import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for ParkingSpot entities.
 * Contains queries for filtering by lot, status, and availability counts.
 */
public interface SpotRepository extends JpaRepository<ParkingSpot, Long> {

    /* ───── Query by lot ───── */

    List<ParkingSpot> findByLotIdOrderByFloorAscSpotNumberAsc(Long lotId);

    List<ParkingSpot> findByLotIdAndStatus(Long lotId, SpotStatus status);

    List<ParkingSpot> findByLotIdAndSpotType(Long lotId, SpotType spotType);

    List<ParkingSpot> findByLotIdAndVehicleType(Long lotId, VehicleType vehicleType);

    /* ───── Counts ───── */

    long countByLotIdAndStatus(Long lotId, SpotStatus status);

    long countByLotId(Long lotId);

    /* ───── Filters ───── */

    List<ParkingSpot> findByIsEVChargingTrue();

    List<ParkingSpot> findByLotIdAndIsHandicappedTrue(Long lotId);

    /* ───── Bulk operations ───── */

    @Modifying
    @Query("DELETE FROM ParkingSpot s WHERE s.lotId = :lotId")
    void deleteAllByLotId(@Param("lotId") Long lotId);
}
