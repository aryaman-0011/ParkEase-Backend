package com.parkease.vehicle_service.repository;

import com.parkease.vehicle_service.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Vehicle entities.
 * Contains queries for user-specific vehicle lookups.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerId(Long ownerId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByVehicleType(String vehicleType);

    List<Vehicle> findByIsEV(Boolean isEV);

    boolean existsByLicensePlate(String licensePlate);

    void deleteByVehicleId(Long vehicleId);
}
