package com.parkease.parkinglot_service.repository;

import com.parkease.parkinglot_service.entity.ParkingLot;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    /* ───── Driver-facing queries ───── */

    @Query("SELECT p FROM ParkingLot p WHERE p.approved = true " +
            "AND LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<ParkingLot> searchByCity(@Param("city") String city);

    List<ParkingLot> findByApprovedTrueAndOpenTrue();

    List<ParkingLot> findByApprovedTrue();

    /* ───── Manager-facing queries ───── */

    List<ParkingLot> findByManagerIdOrderByCreatedAtDesc(Long managerId);

    /* ───── Admin-facing queries ───── */

    List<ParkingLot> findByApprovedFalseOrderByCreatedAtDesc();

    Page<ParkingLot> findAll(Pageable pageable);

    /* ───── Atomic spot counter operations ───── */

    @Modifying
    @Query("UPDATE ParkingLot p SET p.availableSpots = p.availableSpots - 1 " +
            "WHERE p.id = :lotId AND p.availableSpots > 0")
    int decrementAvailableSpots(@Param("lotId") Long lotId);

    @Modifying
    @Query("UPDATE ParkingLot p SET p.availableSpots = p.availableSpots + 1 " +
            "WHERE p.id = :lotId AND p.availableSpots < p.totalSpots")
    int incrementAvailableSpots(@Param("lotId") Long lotId);

    /* ───── Account deletion cleanup ───── */

    @Modifying
    @Query("DELETE FROM ParkingLot p WHERE p.managerId = :managerId")
    void deleteAllByManagerId(@Param("managerId") Long managerId);
}
