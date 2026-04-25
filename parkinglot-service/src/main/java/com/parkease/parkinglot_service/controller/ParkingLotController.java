package com.parkease.parkinglot_service.controller;

import com.parkease.parkinglot_service.dto.ApiMessageResponse;
import com.parkease.parkinglot_service.dto.CreateLotRequest;
import com.parkease.parkinglot_service.dto.LotResponse;
import com.parkease.parkinglot_service.dto.UpdateLotRequest;
import com.parkease.parkinglot_service.service.ParkingLotService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// REST controller for parking lot CRUD — manager creates lots, admin approves, drivers search
@RestController
@RequestMapping("/lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService lotService;

    // ── Manager endpoints ──

    // Create a new parking lot — requires manager's user ID from gateway header
    @PostMapping
    public ResponseEntity<LotResponse> createLot(
            @RequestHeader("X-User-Id") Long managerId,
            @Valid @RequestBody CreateLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lotService.createLot(managerId, request));
    }

    // Update lot details (name, address, pricing, coordinates, etc.)
    @PutMapping("/{id}")
    public ResponseEntity<LotResponse> updateLot(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateLotRequest request) {
        return ResponseEntity.ok(lotService.updateLot(managerId, id, request));
    }

    // Toggle lot open/closed status
    @PutMapping("/{id}/toggle")
    public ResponseEntity<LotResponse> toggleOpenClose(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id) {
        return ResponseEntity.ok(lotService.toggleOpenClose(managerId, id));
    }

    // Get all lots owned by the authenticated manager
    @GetMapping("/manager")
    public ResponseEntity<List<LotResponse>> getMyLots(
            @RequestHeader("X-User-Id") Long managerId) {
        return ResponseEntity.ok(lotService.getMyLots(managerId));
    }

    // Delete a lot — only the owner manager can delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> deleteLot(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id) {
        lotService.deleteLot(managerId, id);
        return ResponseEntity.ok(new ApiMessageResponse("Lot deleted successfully"));
    }

    // ── Driver / Guest endpoints ──

    // Search lots by city name
    @GetMapping("/search")
    public ResponseEntity<List<LotResponse>> searchByCity(
            @RequestParam String city) {
        return ResponseEntity.ok(lotService.searchByCity(city));
    }

    // Find lots within radius (km) of given latitude/longitude
    @GetMapping("/nearby")
    public ResponseEntity<List<LotResponse>> findNearbyLots(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10") Double radius) {
        return ResponseEntity.ok(lotService.findNearbyLots(lat, lng, radius));
    }

    // Get a single lot by its ID
    @GetMapping("/{id}")
    public ResponseEntity<LotResponse> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.getLotById(id));
    }

    // Get all approved (publicly visible) lots
    @GetMapping
    public ResponseEntity<List<LotResponse>> getAllApprovedLots() {
        return ResponseEntity.ok(lotService.getAllApprovedLots());
    }

    // ── Admin endpoints ──

    // Get lots pending admin approval
    @GetMapping("/pending")
    public ResponseEntity<List<LotResponse>> getPendingLots() {
        return ResponseEntity.ok(lotService.getPendingLots());
    }

    // Approve a pending lot — makes it visible to drivers
    @PutMapping("/{id}/approve")
    public ResponseEntity<LotResponse> approveLot(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.approveLot(id));
    }

    // Reject and remove a pending lot
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiMessageResponse> rejectLot(@PathVariable Long id) {
        lotService.rejectLot(id);
        return ResponseEntity.ok(new ApiMessageResponse("Lot rejected and removed successfully"));
    }

    // Get all lots regardless of status (admin view)
    @GetMapping("/all")
    public ResponseEntity<List<LotResponse>> getAllLots() {
        return ResponseEntity.ok(lotService.getAllLots());
    }

    // Admin force-delete any lot
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<ApiMessageResponse> deleteLotAsAdmin(@PathVariable Long id) {
        lotService.deleteLotAsAdmin(id);
        return ResponseEntity.ok(new ApiMessageResponse("Lot deleted by admin successfully"));
    }

    // Get lots by a specific manager ID (admin view)
    @GetMapping("/by-manager/{managerId}")
    public ResponseEntity<List<LotResponse>> getLotsByManagerId(@PathVariable Long managerId) {
        return ResponseEntity.ok(lotService.getLotsByManagerId(managerId));
    }

    // ── Internal / Inter-service endpoints (called by spot-service, booking-service) ──

    // Decrement available spot count when a spot is booked
    @PutMapping("/{id}/decrement-spot")
    public ResponseEntity<ApiMessageResponse> decrementSpot(@PathVariable Long id) {
        boolean success = lotService.decrementAvailableSpots(id);
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiMessageResponse("No available spots to decrement"));
        }
        return ResponseEntity.ok(new ApiMessageResponse("Spot decremented successfully"));
    }

    // Increment available spot count when a spot is released
    @PutMapping("/{id}/increment-spot")
    public ResponseEntity<ApiMessageResponse> incrementSpot(@PathVariable Long id) {
        boolean success = lotService.incrementAvailableSpots(id);
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiMessageResponse("Available spots already at maximum"));
        }
        return ResponseEntity.ok(new ApiMessageResponse("Spot incremented successfully"));
    }

    // Sync total/available spot counts from spot-service (called after bulk spot operations)
    @PutMapping("/{id}/sync-spots")
    public ResponseEntity<ApiMessageResponse> syncSpotCounts(
            @PathVariable Long id,
            @RequestParam int total,
            @RequestParam int available) {
        lotService.syncSpotCounts(id, total, available);
        return ResponseEntity.ok(new ApiMessageResponse("Spot counts synced successfully"));
    }

    // ── Internal: Account deletion cleanup ──

    // Delete all lots owned by a manager (called when manager account is deleted)
    @DeleteMapping("/manager/{managerId}")
    public ResponseEntity<ApiMessageResponse> deleteAllLotsByManager(@PathVariable Long managerId) {
        lotService.deleteAllLotsByManager(managerId);
        return ResponseEntity.ok(new ApiMessageResponse("All lots deleted for manager: " + managerId));
    }
}
