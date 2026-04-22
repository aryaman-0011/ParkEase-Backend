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

@RestController
@RequestMapping("/lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService lotService;

    /* ───── Manager endpoints ───── */

    @PostMapping
    public ResponseEntity<LotResponse> createLot(
            @RequestHeader("X-User-Id") Long managerId,
            @Valid @RequestBody CreateLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lotService.createLot(managerId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LotResponse> updateLot(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateLotRequest request) {
        return ResponseEntity.ok(lotService.updateLot(managerId, id, request));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<LotResponse> toggleOpenClose(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id) {
        return ResponseEntity.ok(lotService.toggleOpenClose(managerId, id));
    }

    @GetMapping("/manager")
    public ResponseEntity<List<LotResponse>> getMyLots(
            @RequestHeader("X-User-Id") Long managerId) {
        return ResponseEntity.ok(lotService.getMyLots(managerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> deleteLot(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id) {
        lotService.deleteLot(managerId, id);
        return ResponseEntity.ok(new ApiMessageResponse("Lot deleted successfully"));
    }

    /* ───── Driver / Guest endpoints ───── */

    @GetMapping("/search")
    public ResponseEntity<List<LotResponse>> searchByCity(
            @RequestParam String city) {
        return ResponseEntity.ok(lotService.searchByCity(city));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<LotResponse>> findNearbyLots(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10") Double radius) {
        return ResponseEntity.ok(lotService.findNearbyLots(lat, lng, radius));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LotResponse> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.getLotById(id));
    }

    @GetMapping
    public ResponseEntity<List<LotResponse>> getAllApprovedLots() {
        return ResponseEntity.ok(lotService.getAllApprovedLots());
    }

    /* ───── Admin endpoints ───── */

    @GetMapping("/pending")
    public ResponseEntity<List<LotResponse>> getPendingLots() {
        return ResponseEntity.ok(lotService.getPendingLots());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LotResponse> approveLot(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.approveLot(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiMessageResponse> rejectLot(@PathVariable Long id) {
        lotService.rejectLot(id);
        return ResponseEntity.ok(new ApiMessageResponse("Lot rejected and removed successfully"));
    }

    @GetMapping("/all")
    public ResponseEntity<List<LotResponse>> getAllLots() {
        return ResponseEntity.ok(lotService.getAllLots());
    }

    @DeleteMapping("/{id}/admin")
    public ResponseEntity<ApiMessageResponse> deleteLotAsAdmin(@PathVariable Long id) {
        lotService.deleteLotAsAdmin(id);
        return ResponseEntity.ok(new ApiMessageResponse("Lot deleted by admin successfully"));
    }

    @GetMapping("/by-manager/{managerId}")
    public ResponseEntity<List<LotResponse>> getLotsByManagerId(@PathVariable Long managerId) {
        return ResponseEntity.ok(lotService.getLotsByManagerId(managerId));
    }

    /* ───── Internal / Inter-service endpoints ───── */

    @PutMapping("/{id}/decrement-spot")
    public ResponseEntity<ApiMessageResponse> decrementSpot(@PathVariable Long id) {
        boolean success = lotService.decrementAvailableSpots(id);
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiMessageResponse("No available spots to decrement"));
        }
        return ResponseEntity.ok(new ApiMessageResponse("Spot decremented successfully"));
    }

    @PutMapping("/{id}/increment-spot")
    public ResponseEntity<ApiMessageResponse> incrementSpot(@PathVariable Long id) {
        boolean success = lotService.incrementAvailableSpots(id);
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiMessageResponse("Available spots already at maximum"));
        }
        return ResponseEntity.ok(new ApiMessageResponse("Spot incremented successfully"));
    }

    @PutMapping("/{id}/sync-spots")
    public ResponseEntity<ApiMessageResponse> syncSpotCounts(
            @PathVariable Long id,
            @RequestParam int total,
            @RequestParam int available) {
        lotService.syncSpotCounts(id, total, available);
        return ResponseEntity.ok(new ApiMessageResponse("Spot counts synced successfully"));
    }

    /* ───── Internal: Account deletion cleanup ───── */

    @DeleteMapping("/manager/{managerId}")
    public ResponseEntity<ApiMessageResponse> deleteAllLotsByManager(@PathVariable Long managerId) {
        lotService.deleteAllLotsByManager(managerId);
        return ResponseEntity.ok(new ApiMessageResponse("All lots deleted for manager: " + managerId));
    }
}
