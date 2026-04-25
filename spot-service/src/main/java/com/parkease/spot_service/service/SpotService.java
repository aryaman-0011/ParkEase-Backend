package com.parkease.spot_service.service;

import com.parkease.spot_service.dto.*;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;

import java.util.List;

/**
 * Service interface defining parking spot business operations.
 * Includes CRUD, status transitions, and count aggregations.
 */
public interface SpotService {

    /* ───── CRUD ───── */
    SpotResponse addSpot(CreateSpotRequest request);
    List<SpotResponse> addBulkSpots(BulkCreateSpotRequest request);
    SpotResponse getSpotById(Long spotId);
    SpotResponse updateSpot(Long spotId, UpdateSpotRequest request);
    void deleteSpot(Long spotId);

    /* ───── Query by lot ───── */
    List<SpotResponse> getSpotsByLot(Long lotId);
    List<SpotResponse> getAvailableSpots(Long lotId);
    List<SpotResponse> getSpotsByLotAndType(Long lotId, SpotType spotType);
    List<SpotResponse> getSpotsByLotAndVehicleType(Long lotId, VehicleType vehicleType);

    /* ───── Counts ───── */
    SpotCountResponse getSpotCounts(Long lotId);

    /* ───── Status transitions ───── */
    SpotResponse reserveSpot(Long spotId);
    SpotResponse occupySpot(Long spotId);
    SpotResponse releaseSpot(Long spotId);

    /* ───── Bulk operations ───── */
    void deleteAllSpotsByLot(Long lotId);
}
