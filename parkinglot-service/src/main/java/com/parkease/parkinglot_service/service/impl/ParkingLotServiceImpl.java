package com.parkease.parkinglot_service.service.impl;

import com.parkease.parkinglot_service.client.AuthServiceClient;
import com.parkease.parkinglot_service.dto.CreateLotRequest;
import com.parkease.parkinglot_service.dto.LotResponse;
import com.parkease.parkinglot_service.dto.UpdateLotRequest;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.event.NotificationEvent;
import com.parkease.parkinglot_service.event.NotificationEventProducer;
import com.parkease.parkinglot_service.exception.BadRequestException;
import com.parkease.parkinglot_service.exception.ResourceNotFoundException;
import com.parkease.parkinglot_service.exception.UnauthorizedException;
import com.parkease.parkinglot_service.repository.ParkingLotRepository;
import com.parkease.parkinglot_service.service.ParkingLotService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository lotRepository;
    private final NotificationEventProducer notificationProducer;
    private final AuthServiceClient authServiceClient;

    /* ───── Manager operations ───── */

    @Override
    public LotResponse createLot(Long managerId, CreateLotRequest request) {
        ParkingLot lot = ParkingLot.builder()
                .managerId(managerId)
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .state(request.getState() != null ? request.getState().trim() : null)
                .zipCode(request.getZipCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .totalSpots(request.getTotalSpots())
                .availableSpots(request.getTotalSpots())
                .pricePerHour(request.getPricePerHour())
                .approved(false)
                .open(false)
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .build();

        ParkingLot saved = lotRepository.save(lot);

        // Notify all admins about new pending lot
        notifyAdmins(saved);

        return LotResponse.from(saved);
    }

    @Override
    public LotResponse updateLot(Long managerId, Long lotId, UpdateLotRequest request) {
        ParkingLot lot = getOwnedLot(managerId, lotId);

        if (request.getName() != null) lot.setName(request.getName().trim());
        if (request.getAddress() != null) lot.setAddress(request.getAddress().trim());
        if (request.getCity() != null) lot.setCity(request.getCity().trim());
        if (request.getState() != null) lot.setState(request.getState().trim());
        if (request.getZipCode() != null) lot.setZipCode(request.getZipCode());
        if (request.getLatitude() != null) lot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) lot.setLongitude(request.getLongitude());
        if (request.getPricePerHour() != null) lot.setPricePerHour(request.getPricePerHour());
        if (request.getImageUrl() != null) lot.setImageUrl(request.getImageUrl());
        if (request.getDescription() != null) lot.setDescription(request.getDescription());

        return LotResponse.from(lotRepository.save(lot));
    }

    @Override
    public LotResponse toggleOpenClose(Long managerId, Long lotId) {
        ParkingLot lot = getOwnedLot(managerId, lotId);

        if (!lot.isApproved()) {
            throw new BadRequestException("Cannot open/close a lot that has not been approved by admin");
        }

        lot.setOpen(!lot.isOpen());
        return LotResponse.from(lotRepository.save(lot));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> getMyLots(Long managerId) {
        return lotRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(LotResponse::from)
                .toList();
    }

    @Override
    public void deleteLot(Long managerId, Long lotId) {
        ParkingLot lot = getOwnedLot(managerId, lotId);
        lotRepository.delete(lot);
    }

    /* ───── Driver / Guest operations ───── */

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> searchByCity(String city) {
        return lotRepository.searchByCity(city.trim()).stream()
                .map(LotResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> findNearbyLots(Double latitude, Double longitude, Double radiusKm) {
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : 10.0;

        return lotRepository.findByApprovedTrue().stream()
                .map(lot -> {
                    double distance = haversineDistance(latitude, longitude, lot.getLatitude(), lot.getLongitude());
                    return new Object[]{ lot, distance };
                })
                .filter(arr -> (double) arr[1] <= radius)
                .sorted(Comparator.comparingDouble(arr -> (double) arr[1]))
                .map(arr -> LotResponse.from((ParkingLot) arr[0], (double) arr[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LotResponse getLotById(Long lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));
        return LotResponse.from(lot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> getAllApprovedLots() {
        return lotRepository.findByApprovedTrue().stream()
                .map(LotResponse::from)
                .toList();
    }

    /* ───── Admin operations ───── */

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> getPendingLots() {
        return lotRepository.findByApprovedFalseOrderByCreatedAtDesc().stream()
                .map(LotResponse::from)
                .toList();
    }

    @Override
    public LotResponse approveLot(Long lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));

        if (lot.isApproved()) {
            throw new BadRequestException("Lot is already approved");
        }

        lot.setApproved(true);
        ParkingLot saved = lotRepository.save(lot);

        // Notify the manager that their lot was approved
        notificationProducer.publish(NotificationEvent.builder()
                .recipientId(saved.getManagerId())
                .type("LOT_APPROVED")
                .title("Lot Approved!")
                .message("Your lot '" + saved.getName() + "' has been approved")
                .channel("APP")
                .relatedId(saved.getId())
                .relatedType("LOT")
                .build());

        return LotResponse.from(saved);
    }

    @Override
    public LotResponse rejectLot(Long lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));

        if (lot.isApproved()) {
            throw new BadRequestException("Cannot reject an already approved lot");
        }

        // Notify the manager that their lot was rejected (before deleting)
        notificationProducer.publish(NotificationEvent.builder()
                .recipientId(lot.getManagerId())
                .type("LOT_REJECTED")
                .title("Lot Rejected")
                .message("Your lot '" + lot.getName() + "' was rejected by admin")
                .channel("APP")
                .relatedId(lot.getId())
                .relatedType("LOT")
                .build());

        lotRepository.delete(lot);
        return LotResponse.from(lot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> getAllLots() {
        return lotRepository.findAll().stream()
                .map(LotResponse::from)
                .toList();
    }

    @Override
    public void deleteLotAsAdmin(Long lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));
        lotRepository.delete(lot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotResponse> getLotsByManagerId(Long managerId) {
        return lotRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(LotResponse::from)
                .toList();
    }

    /* ───── Internal / Inter-service operations ───── */

    @Override
    public boolean decrementAvailableSpots(Long lotId) {
        return lotRepository.decrementAvailableSpots(lotId) > 0;
    }

    @Override
    public boolean incrementAvailableSpots(Long lotId) {
        return lotRepository.incrementAvailableSpots(lotId) > 0;
    }

    @Override
    public void syncSpotCounts(Long lotId, int totalSpots, int availableSpots) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));
        lot.setTotalSpots(totalSpots);
        lot.setAvailableSpots(availableSpots);
        lotRepository.save(lot);
    }

    /* ───── Account deletion cleanup ───── */

    @Override
    public void deleteAllLotsByManager(Long managerId) {
        lotRepository.deleteAllByManagerId(managerId);
    }

    /* ───── Helper methods ───── */

    private ParkingLot getOwnedLot(Long managerId, Long lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + lotId));

        if (!lot.getManagerId().equals(managerId)) {
            throw new UnauthorizedException("You are not authorized to manage this lot");
        }

        return lot;
    }

    /**
     * Haversine formula to calculate distance between two GPS coordinates.
     * Returns distance in kilometers.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Fetch admin user IDs from auth-service and publish a notification for each.
     */
    @SuppressWarnings("unchecked")
    private void notifyAdmins(ParkingLot lot) {
        try {
            Map<String, Object> response = authServiceClient.getAdminIds();
            if (response != null && response.get("ids") != null) {
                List<Integer> adminIds = (List<Integer>) response.get("ids");
                for (Integer adminId : adminIds) {
                    notificationProducer.publish(NotificationEvent.builder()
                            .recipientId(adminId.longValue())
                            .type("NEW_LOT_PENDING")
                            .title("New Lot Pending Approval")
                            .message("'" + lot.getName() + "' in " + lot.getCity() + " submitted for approval")
                            .channel("APP")
                            .relatedId(lot.getId())
                            .relatedType("LOT")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Could not notify admins: {}", e.getMessage());
        }
    }
}
