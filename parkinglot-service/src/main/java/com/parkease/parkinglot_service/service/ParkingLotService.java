package com.parkease.parkinglot_service.service;

import com.parkease.parkinglot_service.dto.CreateLotRequest;
import com.parkease.parkinglot_service.dto.LotResponse;
import com.parkease.parkinglot_service.dto.UpdateLotRequest;
import java.util.List;

/**
 * Service interface defining parking lot business operations.
 * Includes CRUD, search, and availability management methods.
 */
public interface ParkingLotService {

    /* ───── Manager operations ───── */

    LotResponse createLot(Long managerId, CreateLotRequest request);

    LotResponse updateLot(Long managerId, Long lotId, UpdateLotRequest request);

    LotResponse toggleOpenClose(Long managerId, Long lotId);

    List<LotResponse> getMyLots(Long managerId);

    void deleteLot(Long managerId, Long lotId);

    /* ───── Driver / Guest operations ───── */

    List<LotResponse> searchByCity(String city);

    List<LotResponse> findNearbyLots(Double latitude, Double longitude, Double radiusKm);

    LotResponse getLotById(Long lotId);

    List<LotResponse> getAllApprovedLots();

    /* ───── Admin operations ───── */

    List<LotResponse> getPendingLots();

    LotResponse approveLot(Long lotId);

    LotResponse rejectLot(Long lotId);

    List<LotResponse> getAllLots();

    void deleteLotAsAdmin(Long lotId);

    List<LotResponse> getLotsByManagerId(Long managerId);

    /* ───── Internal / Inter-service operations ───── */

    boolean decrementAvailableSpots(Long lotId);

    boolean incrementAvailableSpots(Long lotId);

    void syncSpotCounts(Long lotId, int totalSpots, int availableSpots);

    /* ───── Account deletion cleanup ───── */

    void deleteAllLotsByManager(Long managerId);
}
