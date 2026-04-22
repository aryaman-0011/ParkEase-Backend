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

@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    /* ═══════════════════════════════════════
       CREATE
       ═══════════════════════════════════════ */

    /** Add a single parking spot */
    @PostMapping
    public ResponseEntity<SpotResponse> addSpot(@Valid @RequestBody CreateSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.addSpot(request));
    }

    /** Add multiple spots at once (bulk) */
    @PostMapping("/bulk")
    public ResponseEntity<List<SpotResponse>> addBulkSpots(@Valid @RequestBody BulkCreateSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.addBulkSpots(request));
    }

    /* ═══════════════════════════════════════
       READ
       ═══════════════════════════════════════ */

    /** Get a single spot by ID */
    @GetMapping("/{spotId}")
    public ResponseEntity<SpotResponse> getSpotById(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.getSpotById(spotId));
    }

    /** Get all spots in a lot (sorted by floor + number) */
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<SpotResponse>> getSpotsByLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getSpotsByLot(lotId));
    }

    /** Get only available spots in a lot */
    @GetMapping("/lot/{lotId}/available")
    public ResponseEntity<List<SpotResponse>> getAvailableSpots(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getAvailableSpots(lotId));
    }

    /** Get spots filtered by spot type */
    @GetMapping("/lot/{lotId}/type/{spotType}")
    public ResponseEntity<List<SpotResponse>> getSpotsByType(
            @PathVariable Long lotId, @PathVariable SpotType spotType) {
        return ResponseEntity.ok(spotService.getSpotsByLotAndType(lotId, spotType));
    }

    /** Get spots filtered by vehicle type */
    @GetMapping("/lot/{lotId}/vehicle/{vehicleType}")
    public ResponseEntity<List<SpotResponse>> getSpotsByVehicleType(
            @PathVariable Long lotId, @PathVariable VehicleType vehicleType) {
        return ResponseEntity.ok(spotService.getSpotsByLotAndVehicleType(lotId, vehicleType));
    }

    /** Get spot count breakdown for a lot */
    @GetMapping("/lot/{lotId}/count")
    public ResponseEntity<SpotCountResponse> getSpotCounts(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getSpotCounts(lotId));
    }

    /* ═══════════════════════════════════════
       UPDATE
       ═══════════════════════════════════════ */

    /** Update spot details */
    @PutMapping("/{spotId}")
    public ResponseEntity<SpotResponse> updateSpot(
            @PathVariable Long spotId, @Valid @RequestBody UpdateSpotRequest request) {
        return ResponseEntity.ok(spotService.updateSpot(spotId, request));
    }

    /** Reserve a spot (AVAILABLE → RESERVED) */
    @PutMapping("/{spotId}/reserve")
    public ResponseEntity<SpotResponse> reserveSpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.reserveSpot(spotId));
    }

    /** Occupy a spot — driver checks in (RESERVED → OCCUPIED) */
    @PutMapping("/{spotId}/occupy")
    public ResponseEntity<SpotResponse> occupySpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.occupySpot(spotId));
    }

    /** Release a spot — checkout or cancel (→ AVAILABLE) */
    @PutMapping("/{spotId}/release")
    public ResponseEntity<SpotResponse> releaseSpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.releaseSpot(spotId));
    }

    /* ═══════════════════════════════════════
       DELETE
       ═══════════════════════════════════════ */

    /** Delete a single spot */
    @DeleteMapping("/{spotId}")
    public ResponseEntity<ApiMessageResponse> deleteSpot(@PathVariable Long spotId) {
        spotService.deleteSpot(spotId);
        return ResponseEntity.ok(new ApiMessageResponse("Spot deleted successfully"));
    }

    /** Delete all spots in a lot (used during lot deletion) */
    @DeleteMapping("/lot/{lotId}")
    public ResponseEntity<ApiMessageResponse> deleteAllSpotsByLot(@PathVariable Long lotId) {
        spotService.deleteAllSpotsByLot(lotId);
        return ResponseEntity.ok(new ApiMessageResponse("All spots for lot " + lotId + " deleted successfully"));
    }
}
