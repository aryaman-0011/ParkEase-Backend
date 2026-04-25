package com.parkease.spot_service.service.impl;

import com.parkease.spot_service.client.LotSyncClient;
import com.parkease.spot_service.dto.*;
import com.parkease.spot_service.entity.ParkingSpot;
import com.parkease.spot_service.enums.SpotStatus;
import com.parkease.spot_service.enums.SpotType;
import com.parkease.spot_service.enums.VehicleType;
import com.parkease.spot_service.exception.BadRequestException;
import com.parkease.spot_service.exception.ResourceNotFoundException;
import com.parkease.spot_service.repository.SpotRepository;
import com.parkease.spot_service.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of SpotService with spot management logic.
 * Syncs availability counts with the Parking Lot service after status changes.
 */
@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final SpotRepository spotRepository;
    private final LotSyncClient lotSyncClient;

    /* ═══════════════════════════════════════
       CRUD
       ═══════════════════════════════════════ */

    @Override
    @Transactional
    public SpotResponse addSpot(CreateSpotRequest req) {
        ParkingSpot spot = ParkingSpot.builder()
                .lotId(req.getLotId())
                .spotNumber(req.getSpotNumber().trim())
                .floor(req.getFloor())
                .spotType(req.getSpotType())
                .vehicleType(req.getVehicleType())
                .isHandicapped(req.getIsHandicapped())
                .isEVCharging(req.getIsEVCharging())
                .pricePerHour(req.getPricePerHour())
                .status(SpotStatus.AVAILABLE)
                .build();
        SpotResponse response = SpotResponse.from(spotRepository.save(spot));
        lotSyncClient.syncCounts(req.getLotId());
        return response;
    }

    @Override
    @Transactional
    public List<SpotResponse> addBulkSpots(BulkCreateSpotRequest req) {
        List<ParkingSpot> spots = new ArrayList<>();
        String prefix = (req.getPrefix() != null && !req.getPrefix().isBlank())
                ? req.getPrefix().trim() + "-"
                : "";
        int start = (req.getStartFrom() != null) ? req.getStartFrom() : 1;

        for (int i = 0; i < req.getCount(); i++) {
            String number = prefix + String.format("%02d", start + i);
            ParkingSpot spot = ParkingSpot.builder()
                    .lotId(req.getLotId())
                    .spotNumber(number)
                    .floor(req.getFloor())
                    .spotType(req.getSpotType())
                    .vehicleType(req.getVehicleType())
                    .isHandicapped(req.getIsHandicapped())
                    .isEVCharging(req.getIsEVCharging())
                    .pricePerHour(req.getPricePerHour())
                    .status(SpotStatus.AVAILABLE)
                    .build();
            spots.add(spot);
        }

        List<SpotResponse> result = spotRepository.saveAll(spots).stream()
                .map(SpotResponse::from)
                .toList();
        lotSyncClient.syncCounts(req.getLotId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SpotResponse getSpotById(Long spotId) {
        return SpotResponse.from(findSpotOrThrow(spotId));
    }

    @Override
    @Transactional
    public SpotResponse updateSpot(Long spotId, UpdateSpotRequest req) {
        ParkingSpot spot = findSpotOrThrow(spotId);

        if (req.getSpotNumber() != null) spot.setSpotNumber(req.getSpotNumber().trim());
        if (req.getFloor() != null) spot.setFloor(req.getFloor());
        if (req.getSpotType() != null) spot.setSpotType(req.getSpotType());
        if (req.getVehicleType() != null) spot.setVehicleType(req.getVehicleType());
        if (req.getIsHandicapped() != null) spot.setIsHandicapped(req.getIsHandicapped());
        if (req.getIsEVCharging() != null) spot.setIsEVCharging(req.getIsEVCharging());
        if (req.getPricePerHour() != null) spot.setPricePerHour(req.getPricePerHour());

        return SpotResponse.from(spotRepository.save(spot));
    }

    @Override
    @Transactional
    public void deleteSpot(Long spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);
        Long lotId = spot.getLotId();
        spotRepository.deleteById(spotId);
        lotSyncClient.syncCounts(lotId);
    }

    /* ═══════════════════════════════════════
       Query by lot
       ═══════════════════════════════════════ */

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getSpotsByLot(Long lotId) {
        return spotRepository.findByLotIdOrderByFloorAscSpotNumberAsc(lotId).stream()
                .map(SpotResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getAvailableSpots(Long lotId) {
        return spotRepository.findByLotIdAndStatus(lotId, SpotStatus.AVAILABLE).stream()
                .map(SpotResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getSpotsByLotAndType(Long lotId, SpotType spotType) {
        return spotRepository.findByLotIdAndSpotType(lotId, spotType).stream()
                .map(SpotResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getSpotsByLotAndVehicleType(Long lotId, VehicleType vehicleType) {
        return spotRepository.findByLotIdAndVehicleType(lotId, vehicleType).stream()
                .map(SpotResponse::from)
                .toList();
    }

    /* ═══════════════════════════════════════
       Counts
       ═══════════════════════════════════════ */

    @Override
    @Transactional(readOnly = true)
    public SpotCountResponse getSpotCounts(Long lotId) {
        return SpotCountResponse.builder()
                .lotId(lotId)
                .total(spotRepository.countByLotId(lotId))
                .available(spotRepository.countByLotIdAndStatus(lotId, SpotStatus.AVAILABLE))
                .reserved(spotRepository.countByLotIdAndStatus(lotId, SpotStatus.RESERVED))
                .occupied(spotRepository.countByLotIdAndStatus(lotId, SpotStatus.OCCUPIED))
                .build();
    }

    /* ═══════════════════════════════════════
       Status transitions
       ═══════════════════════════════════════ */

    @Override
    @Transactional
    public SpotResponse reserveSpot(Long spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);
        if (spot.getStatus() != SpotStatus.AVAILABLE) {
            throw new BadRequestException("Spot " + spot.getSpotNumber() +
                    " is not available (current: " + spot.getStatus() + ")");
        }
        spot.setStatus(SpotStatus.RESERVED);
        SpotResponse response = SpotResponse.from(spotRepository.save(spot));
        lotSyncClient.syncCounts(spot.getLotId());
        return response;
    }

    @Override
    @Transactional
    public SpotResponse occupySpot(Long spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);
        if (spot.getStatus() != SpotStatus.RESERVED) {
            throw new BadRequestException("Spot " + spot.getSpotNumber() +
                    " must be RESERVED before occupying (current: " + spot.getStatus() + ")");
        }
        spot.setStatus(SpotStatus.OCCUPIED);
        SpotResponse response = SpotResponse.from(spotRepository.save(spot));
        lotSyncClient.syncCounts(spot.getLotId());
        return response;
    }

    @Override
    @Transactional
    public SpotResponse releaseSpot(Long spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);
        if (spot.getStatus() == SpotStatus.AVAILABLE) {
            throw new BadRequestException("Spot " + spot.getSpotNumber() + " is already available");
        }
        spot.setStatus(SpotStatus.AVAILABLE);
        SpotResponse response = SpotResponse.from(spotRepository.save(spot));
        lotSyncClient.syncCounts(spot.getLotId());
        return response;
    }

    /* ═══════════════════════════════════════
       Bulk operations
       ═══════════════════════════════════════ */

    @Override
    @Transactional
    public void deleteAllSpotsByLot(Long lotId) {
        spotRepository.deleteAllByLotId(lotId);
        lotSyncClient.syncCounts(lotId);
    }

    /* ═══════════════════════════════════════
       Helpers
       ═══════════════════════════════════════ */

    private ParkingSpot findSpotOrThrow(Long spotId) {
        return spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking spot not found with id: " + spotId));
    }
}
