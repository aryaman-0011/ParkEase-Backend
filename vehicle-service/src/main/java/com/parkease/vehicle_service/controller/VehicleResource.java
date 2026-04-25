package com.parkease.vehicle_service.controller;

import com.parkease.vehicle_service.dto.RegisterVehicleRequest;
import com.parkease.vehicle_service.dto.UpdateVehicleRequest;
import com.parkease.vehicle_service.dto.VehicleResponse;
import com.parkease.vehicle_service.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleResource {

    private final VehicleService vehicleService;

    /** Register a new vehicle */
    @PostMapping
    public ResponseEntity<VehicleResponse> registerVehicle(@Valid @RequestBody RegisterVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.registerVehicle(request));
    }

    /** Get vehicle by ID */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    /** Get all vehicles for an owner */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VehicleResponse>> getByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(ownerId));
    }

    /** Get vehicle by license plate */
    @GetMapping("/plate/{plate}")
    public ResponseEntity<VehicleResponse> getByPlate(@PathVariable String plate) {
        return vehicleService.getByLicensePlate(plate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Update vehicle */
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Long id, @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    /** Delete vehicle */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    /** Get vehicle type by ID */
    @GetMapping("/{id}/type")
    public ResponseEntity<Map<String, String>> getType(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("vehicleType", vehicleService.getVehicleType(id)));
    }

    /** Check if vehicle is EV */
    @GetMapping("/{id}/ev")
    public ResponseEntity<Map<String, Boolean>> isEV(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("isEV", vehicleService.isEVVehicle(id)));
    }

    /** Get vehicles by type (2W, 4W, HEAVY) */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<VehicleResponse>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(vehicleService.getVehiclesByType(type));
    }

    /** Get vehicles by EV status */
    @GetMapping("/ev/{isEV}")
    public ResponseEntity<List<VehicleResponse>> getByEVStatus(@PathVariable Boolean isEV) {
        return ResponseEntity.ok(vehicleService.getByEVStatus(isEV));
    }

    /** Get all vehicles */
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAll() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }
}
