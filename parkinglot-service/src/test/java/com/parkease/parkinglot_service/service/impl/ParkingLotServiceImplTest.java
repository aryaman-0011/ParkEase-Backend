package com.parkease.parkinglot_service.service.impl;

import com.parkease.parkinglot_service.client.AuthServiceClient;
import com.parkease.parkinglot_service.dto.CreateLotRequest;
import com.parkease.parkinglot_service.dto.LotResponse;
import com.parkease.parkinglot_service.dto.UpdateLotRequest;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.event.NotificationEventProducer;
import com.parkease.parkinglot_service.exception.BadRequestException;
import com.parkease.parkinglot_service.exception.ResourceNotFoundException;
import com.parkease.parkinglot_service.exception.UnauthorizedException;
import com.parkease.parkinglot_service.repository.ParkingLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceImplTest {

    @Mock private ParkingLotRepository lotRepository;
    @Mock private NotificationEventProducer notificationProducer;
    @Mock private AuthServiceClient authServiceClient;
    @InjectMocks private ParkingLotServiceImpl parkingLotService;

    private ParkingLot lot;

    @BeforeEach
    void setUp() {
        lot = ParkingLot.builder()
                .id(1L).managerId(10L).name("Downtown Parking")
                .address("123 Main St").city("Bhopal").state("MP")
                .zipCode("462001").latitude(23.2599).longitude(77.4126)
                .totalSpots(50).availableSpots(50).pricePerHour(BigDecimal.valueOf(30.0))
                .approved(false).open(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createLot")
    class CreateLot {
        @Test
        @DisplayName("should create lot with approved=false and open=false")
        void createSuccess() {
            CreateLotRequest req = CreateLotRequest.builder()
                    .name("Downtown Parking").address("123 Main St").city("Bhopal")
                    .state("MP").zipCode("462001").latitude(23.2599).longitude(77.4126)
                    .totalSpots(50).pricePerHour(BigDecimal.valueOf(30.0)).build();

            when(lotRepository.save(any(ParkingLot.class))).thenReturn(lot);

            LotResponse response = parkingLotService.createLot(10L, req);

            assertThat(response.getName()).isEqualTo("Downtown Parking");
            assertThat(response.isApproved()).isFalse();
            assertThat(response.isOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateLot")
    class UpdateLot {
        @Test
        @DisplayName("should update only provided fields")
        void updatePartial() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
            when(lotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UpdateLotRequest req = UpdateLotRequest.builder()
                    .name("Updated Parking").pricePerHour(BigDecimal.valueOf(40.0)).build();

            LotResponse response = parkingLotService.updateLot(10L, 1L, req);

            assertThat(response.getName()).isEqualTo("Updated Parking");
            assertThat(response.getPricePerHour()).isEqualByComparingTo(BigDecimal.valueOf(40.0));
            assertThat(response.getCity()).isEqualTo("Bhopal"); // unchanged
        }

        @Test
        @DisplayName("should throw when not the owner")
        void updateNotOwner() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            assertThatThrownBy(() -> parkingLotService.updateLot(999L, 1L, new UpdateLotRequest()))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("toggleOpenClose")
    class ToggleOpenClose {
        @Test
        @DisplayName("should toggle open state when approved")
        void toggleApproved() {
            lot.setApproved(true);
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
            when(lotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LotResponse response = parkingLotService.toggleOpenClose(10L, 1L);

            assertThat(response.isOpen()).isTrue();
        }

        @Test
        @DisplayName("should throw when not approved")
        void toggleNotApproved() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            assertThatThrownBy(() -> parkingLotService.toggleOpenClose(10L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not been approved");
        }
    }

    @Nested
    @DisplayName("getMyLots")
    class GetMyLots {
        @Test
        @DisplayName("should return lots for manager")
        void myLots() {
            when(lotRepository.findByManagerIdOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(lot));

            List<LotResponse> result = parkingLotService.getMyLots(10L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteLot")
    class DeleteLot {
        @Test
        @DisplayName("should delete owned lot")
        void deleteOwned() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            parkingLotService.deleteLot(10L, 1L);

            verify(lotRepository).delete(lot);
        }

        @Test
        @DisplayName("should throw when not the owner")
        void deleteNotOwner() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            assertThatThrownBy(() -> parkingLotService.deleteLot(999L, 1L))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("searchByCity")
    class SearchByCity {
        @Test
        @DisplayName("should search by city name")
        void search() {
            when(lotRepository.searchByCity("Bhopal")).thenReturn(List.of(lot));

            List<LotResponse> result = parkingLotService.searchByCity("Bhopal");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCity()).isEqualTo("Bhopal");
        }
    }

    @Nested
    @DisplayName("getLotById")
    class GetLotById {
        @Test
        @DisplayName("should return lot when found")
        void found() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            LotResponse response = parkingLotService.getLotById(1L);

            assertThat(response.getName()).isEqualTo("Downtown Parking");
        }

        @Test
        @DisplayName("should throw when not found")
        void notFound() {
            when(lotRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> parkingLotService.getLotById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Admin operations")
    class AdminOps {
        @Test
        @DisplayName("approveLot should set approved=true and notify manager")
        void approve() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
            when(lotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LotResponse response = parkingLotService.approveLot(1L);

            assertThat(response.isApproved()).isTrue();
            verify(notificationProducer).publish(any());
        }

        @Test
        @DisplayName("approveLot should throw when already approved")
        void approveAlready() {
            lot.setApproved(true);
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            assertThatThrownBy(() -> parkingLotService.approveLot(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already approved");
        }

        @Test
        @DisplayName("rejectLot should delete and notify manager")
        void reject() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            parkingLotService.rejectLot(1L);

            verify(notificationProducer).publish(any());
            verify(lotRepository).delete(lot);
        }

        @Test
        @DisplayName("rejectLot should throw when already approved")
        void rejectApproved() {
            lot.setApproved(true);
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            assertThatThrownBy(() -> parkingLotService.rejectLot(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already approved");
        }

        @Test
        @DisplayName("getPendingLots should return unapproved lots")
        void pendingLots() {
            when(lotRepository.findByApprovedFalseOrderByCreatedAtDesc())
                    .thenReturn(List.of(lot));

            assertThat(parkingLotService.getPendingLots()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Spot count sync")
    class SpotSync {
        @Test
        @DisplayName("decrementAvailableSpots should delegate to repository")
        void decrement() {
            when(lotRepository.decrementAvailableSpots(1L)).thenReturn(1);

            assertThat(parkingLotService.decrementAvailableSpots(1L)).isTrue();
        }

        @Test
        @DisplayName("incrementAvailableSpots should return false when no rows affected")
        void incrementNone() {
            when(lotRepository.incrementAvailableSpots(99L)).thenReturn(0);

            assertThat(parkingLotService.incrementAvailableSpots(99L)).isFalse();
        }

        @Test
        @DisplayName("syncSpotCounts should update counts on lot")
        void sync() {
            when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

            parkingLotService.syncSpotCounts(1L, 100, 80);

            assertThat(lot.getTotalSpots()).isEqualTo(100);
            assertThat(lot.getAvailableSpots()).isEqualTo(80);
            verify(lotRepository).save(lot);
        }
    }
}
