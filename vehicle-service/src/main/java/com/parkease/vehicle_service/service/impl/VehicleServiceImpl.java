package com.parkease.vehicle_service.service.impl;

import com.parkease.vehicle_service.dto.RegisterVehicleRequest;
import com.parkease.vehicle_service.dto.UpdateVehicleRequest;
import com.parkease.vehicle_service.dto.VehicleResponse;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.exception.BadRequestException;
import com.parkease.vehicle_service.exception.ResourceNotFoundException;
import com.parkease.vehicle_service.repository.VehicleRepository;
import com.parkease.vehicle_service.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public VehicleResponse registerVehicle(RegisterVehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BadRequestException("Vehicle with license plate '" + request.getLicensePlate() + "' already exists.");
        }

        Vehicle vehicle = Vehicle.builder()
                .ownerId(request.getOwnerId())
                .licensePlate(request.getLicensePlate().toUpperCase().trim())
                .make(request.getMake())
                .model(request.getModel())
                .color(request.getColor())
                .vehicleType(request.getVehicleType().toUpperCase().trim())
                .isEV(request.getIsEV() != null ? request.getIsEV() : false)
                .isActive(true)
                .build();

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle registered: {} — {} {} ({})", vehicle.getLicensePlate(), vehicle.getMake(), vehicle.getModel(), vehicle.getVehicleType());
        return toResponse(vehicle);
    }

    @Override
    public VehicleResponse getVehicleById(Long vehicleId) {
        return toResponse(findVehicle(vehicleId));
    }

    @Override
    public Optional<VehicleResponse> getByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate.toUpperCase().trim())
                .map(this::toResponse);
    }

    @Override
    public List<VehicleResponse> getVehiclesByOwner(Long ownerId) {
        return vehicleRepository.findByOwnerId(ownerId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public VehicleResponse updateVehicle(Long vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = findVehicle(vehicleId);

        if (request.getMake() != null) vehicle.setMake(request.getMake());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getColor() != null) vehicle.setColor(request.getColor());
        if (request.getVehicleType() != null) vehicle.setVehicleType(request.getVehicleType().toUpperCase().trim());
        if (request.getIsEV() != null) vehicle.setIsEV(request.getIsEV());
        if (request.getIsActive() != null) vehicle.setIsActive(request.getIsActive());

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: {} (id={})", vehicle.getLicensePlate(), vehicleId);
        return toResponse(vehicle);
    }

    @Override
    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: {} (id={})", vehicle.getLicensePlate(), vehicleId);
    }

    @Override
    public String getVehicleType(Long vehicleId) {
        return findVehicle(vehicleId).getVehicleType();
    }

    @Override
    public boolean isEVVehicle(Long vehicleId) {
        return Boolean.TRUE.equals(findVehicle(vehicleId).getIsEV());
    }

    @Override
    public List<VehicleResponse> getVehiclesByType(String vehicleType) {
        return vehicleRepository.findByVehicleType(vehicleType.toUpperCase().trim())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<VehicleResponse> getByEVStatus(Boolean isEV) {
        return vehicleRepository.findByIsEV(isEV)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    // ── Helpers ──

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }

    private VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .vehicleId(v.getVehicleId())
                .ownerId(v.getOwnerId())
                .licensePlate(v.getLicensePlate())
                .make(v.getMake())
                .model(v.getModel())
                .color(v.getColor())
                .vehicleType(v.getVehicleType())
                .isEV(v.getIsEV())
                .registeredAt(v.getRegisteredAt())
                .isActive(v.getIsActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
