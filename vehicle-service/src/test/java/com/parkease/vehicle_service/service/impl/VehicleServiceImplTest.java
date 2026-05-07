package com.parkease.vehicle_service.service.impl;

import com.parkease.vehicle_service.dto.RegisterVehicleRequest;
import com.parkease.vehicle_service.dto.UpdateVehicleRequest;
import com.parkease.vehicle_service.dto.VehicleResponse;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.exception.BadRequestException;
import com.parkease.vehicle_service.exception.ResourceNotFoundException;
import com.parkease.vehicle_service.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock private VehicleRepository vehicleRepository;
    @InjectMocks private VehicleServiceImpl vehicleService;

    private Vehicle car;

    @BeforeEach
    void setUp() {
        car = Vehicle.builder()
                .vehicleId(1L).ownerId(10L).licensePlate("MH12AB1234")
                .make("Honda").model("City").color("White")
                .vehicleType("4W").isEV(false).isActive(true)
                .registeredAt(LocalDate.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private RegisterVehicleRequest buildRegisterRequest(Long ownerId, String plate,
            String make, String model, String vehicleType) {
        RegisterVehicleRequest req = new RegisterVehicleRequest();
        req.setOwnerId(ownerId);
        req.setLicensePlate(plate);
        req.setMake(make);
        req.setModel(model);
        req.setVehicleType(vehicleType);
        return req;
    }

    @Nested
    @DisplayName("registerVehicle")
    class RegisterVehicle {
        @Test
        @DisplayName("should register a new vehicle")
        void registerSuccess() {
            RegisterVehicleRequest req = buildRegisterRequest(10L, "MH12AB1234", "Honda", "City", "4W");
            req.setColor("White");
            req.setIsEV(false);

            when(vehicleRepository.existsByLicensePlate("MH12AB1234")).thenReturn(false);
            when(vehicleRepository.save(any(Vehicle.class))).thenReturn(car);

            VehicleResponse response = vehicleService.registerVehicle(req);

            assertThat(response.getLicensePlate()).isEqualTo("MH12AB1234");
            assertThat(response.getMake()).isEqualTo("Honda");
            verify(vehicleRepository).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("should throw when plate already exists")
        void registerDuplicatePlate() {
            RegisterVehicleRequest req = buildRegisterRequest(10L, "MH12AB1234", "Honda", "City", "4W");

            when(vehicleRepository.existsByLicensePlate("MH12AB1234")).thenReturn(true);

            assertThatThrownBy(() -> vehicleService.registerVehicle(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("getVehicleById")
    class GetVehicleById {
        @Test
        @DisplayName("should return vehicle when found")
        void found() {
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(car));

            VehicleResponse response = vehicleService.getVehicleById(1L);

            assertThat(response.getVehicleId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw when not found")
        void notFound() {
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.getVehicleById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getByLicensePlate")
    class GetByLicensePlate {
        @Test
        @DisplayName("should return Optional with vehicle")
        void found() {
            when(vehicleRepository.findByLicensePlate("MH12AB1234")).thenReturn(Optional.of(car));

            Optional<VehicleResponse> result = vehicleService.getByLicensePlate("mh12ab1234");

            assertThat(result).isPresent();
            assertThat(result.get().getLicensePlate()).isEqualTo("MH12AB1234");
        }
    }

    @Nested
    @DisplayName("getVehiclesByOwner")
    class GetVehiclesByOwner {
        @Test
        @DisplayName("should return all vehicles for owner")
        void ownerVehicles() {
            when(vehicleRepository.findByOwnerId(10L)).thenReturn(List.of(car));

            List<VehicleResponse> result = vehicleService.getVehiclesByOwner(10L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateVehicle")
    class UpdateVehicle {
        @Test
        @DisplayName("should update only provided fields")
        void updatePartial() {
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(car));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

            UpdateVehicleRequest req = new UpdateVehicleRequest();
            req.setColor("Red");
            req.setIsEV(true);

            VehicleResponse response = vehicleService.updateVehicle(1L, req);

            assertThat(response.getColor()).isEqualTo("Red");
            assertThat(response.getIsEV()).isTrue();
            assertThat(response.getMake()).isEqualTo("Honda"); // unchanged
        }
    }

    @Nested
    @DisplayName("deleteVehicle")
    class DeleteVehicle {
        @Test
        @DisplayName("should delete existing vehicle")
        void deleteSuccess() {
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(car));

            vehicleService.deleteVehicle(1L);

            verify(vehicleRepository).delete(car);
        }

        @Test
        @DisplayName("should throw when vehicle not found")
        void deleteNotFound() {
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.deleteVehicle(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getVehicleType and isEVVehicle")
    class TypeChecks {
        @Test
        @DisplayName("should return vehicle type")
        void vehicleType() {
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(car));

            assertThat(vehicleService.getVehicleType(1L)).isEqualTo("4W");
        }

        @Test
        @DisplayName("should return EV status")
        void evStatus() {
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(car));

            assertThat(vehicleService.isEVVehicle(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("getVehiclesByType and getByEVStatus")
    class FilterMethods {
        @Test
        @DisplayName("should filter by type")
        void byType() {
            when(vehicleRepository.findByVehicleType("4W")).thenReturn(List.of(car));

            assertThat(vehicleService.getVehiclesByType("4w")).hasSize(1);
        }

        @Test
        @DisplayName("should filter by EV status")
        void byEV() {
            when(vehicleRepository.findByIsEV(false)).thenReturn(List.of(car));

            assertThat(vehicleService.getByEVStatus(false)).hasSize(1);
        }
    }
}
