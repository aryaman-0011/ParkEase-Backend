package com.parkease.spot_service.controller;

import com.parkease.spot_service.dto.*;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.service.SpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST controller for parking spot management — CRUD, status transitions, bulk operations
@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    // ── CREATE ──

    // Add a single parking spot to a lot
    @PostMapping
    public ResponseEntity<SpotResponse> addSpot(@Valid @RequestBody CreateSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.addSpot(request));
    }

    // Add multiple spots at once (e.g. "create 20 COMPACT spots on floor 2")
    @PostMapping("/bulk")
    public ResponseEntity<List<SpotResponse>> addBulkSpots(@Valid @RequestBody BulkCreateSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.addBulkSpots(request));
    }

    // ── READ ──

    // Get a single spot by its ID
    @GetMapping("/{spotId}")
    public ResponseEntity<SpotResponse> getSpotById(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.getSpotById(spotId));
    }

    // Get all spots in a lot (sorted by floor + spot number)
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<SpotResponse>> getSpotsByLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getSpotsByLot(lotId));
    }

    // Get only AVAILABLE spots in a lot
    @GetMapping("/lot/{lotId}/available")
    public ResponseEntity<List<SpotResponse>> getAvailableSpots(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getAvailableSpots(lotId));
    }

    // Get spots filtered by spot type (COMPACT, STANDARD, LARGE, etc.)
    @GetMapping("/lot/{lotId}/type/{spotType}")
    public ResponseEntity<List<SpotResponse>> getSpotsByType(
            @PathVariable Long lotId, @PathVariable SpotType spotType) {
        return ResponseEntity.ok(spotService.getSpotsByLotAndType(lotId, spotType));
    }

    // Get spots filtered by vehicle type (TWO_WHEELER, FOUR_WHEELER, HEAVY)
    @GetMapping("/lot/{lotId}/vehicle/{vehicleType}")
    public ResponseEntity<List<SpotResponse>> getSpotsByVehicleType(
            @PathVariable Long lotId, @PathVariable VehicleType vehicleType) {
        return ResponseEntity.ok(spotService.getSpotsByLotAndVehicleType(lotId, vehicleType));
    }

    // Get spot count breakdown — total, available, reserved, occupied
    @GetMapping("/lot/{lotId}/count")
    public ResponseEntity<SpotCountResponse> getSpotCounts(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getSpotCounts(lotId));
    }

    // ── UPDATE ──

    // Update spot details (price, type, floor, EV/handicapped flags)
    @PutMapping("/{spotId}")
    public ResponseEntity<SpotResponse> updateSpot(
            @PathVariable Long spotId, @Valid @RequestBody UpdateSpotRequest request) {
        return ResponseEntity.ok(spotService.updateSpot(spotId, request));
    }

    // Reserve a spot — transitions AVAILABLE → RESERVED (called by booking-service)
    @PutMapping("/{spotId}/reserve")
    public ResponseEntity<SpotResponse> reserveSpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.reserveSpot(spotId));
    }

    // Occupy a spot — driver checks in, transitions RESERVED → OCCUPIED
    @PutMapping("/{spotId}/occupy")
    public ResponseEntity<SpotResponse> occupySpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.occupySpot(spotId));
    }

    // Release a spot — checkout or cancel, transitions back to AVAILABLE
    @PutMapping("/{spotId}/release")
    public ResponseEntity<SpotResponse> releaseSpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.releaseSpot(spotId));
    }

    // ── DELETE ──

    // Delete a single spot by ID
    @DeleteMapping("/{spotId}")
    public ResponseEntity<ApiMessageResponse> deleteSpot(@PathVariable Long spotId) {
        spotService.deleteSpot(spotId);
        return ResponseEntity.ok(new ApiMessageResponse("Spot deleted successfully"));
    }

    // Delete all spots in a lot (used during lot deletion cascade)
    @DeleteMapping("/lot/{lotId}")
    public ResponseEntity<ApiMessageResponse> deleteAllSpotsByLot(@PathVariable Long lotId) {
        spotService.deleteAllSpotsByLot(lotId);
        return ResponseEntity.ok(new ApiMessageResponse("All spots for lot " + lotId + " deleted successfully"));
    }
}
