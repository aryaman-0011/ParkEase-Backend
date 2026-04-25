package com.parkease.vehicle_service.service;

import com.parkease.vehicle_service.dto.RegisterVehicleRequest;
import com.parkease.vehicle_service.dto.UpdateVehicleRequest;
import com.parkease.vehicle_service.dto.VehicleResponse;

import java.util.List;
import java.util.Optional;

public interface VehicleService {

    VehicleResponse registerVehicle(RegisterVehicleRequest request);

    VehicleResponse getVehicleById(Long vehicleId);

    Optional<VehicleResponse> getByLicensePlate(String licensePlate);

    List<VehicleResponse> getVehiclesByOwner(Long ownerId);

    VehicleResponse updateVehicle(Long vehicleId, UpdateVehicleRequest request);

    void deleteVehicle(Long vehicleId);

    String getVehicleType(Long vehicleId);

    boolean isEVVehicle(Long vehicleId);

    List<VehicleResponse> getVehiclesByType(String vehicleType);

    List<VehicleResponse> getByEVStatus(Boolean isEV);

    List<VehicleResponse> getAllVehicles();
}
