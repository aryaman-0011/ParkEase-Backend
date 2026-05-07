package com.parkease.auth.service.impl;

import com.parkease.auth.dto.AdminUpdateUserRequest;
import com.parkease.auth.dto.UserPageResponse;
import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.dto.UserStatsResponse;
import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.exception.BadRequestException;
import com.parkease.auth.exception.ResourceNotFoundException;
import com.parkease.auth.repository.PasswordResetOtpRepository;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetOtpRepository otpRepository;
    @InjectMocks private AdminServiceImpl adminService;

    private User driver;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        driver = User.builder()
                .id(1L).fullName("John Driver").email("john@test.com")
                .role(Role.DRIVER).provider(AuthProvider.LOCAL).active(true)
                .createdAt(now).updatedAt(now).build();
        manager = User.builder()
                .id(2L).fullName("Jane Manager").email("jane@test.com")
                .role(Role.MANAGER).provider(AuthProvider.LOCAL).active(true)
                .createdAt(now).updatedAt(now).build();
        admin = User.builder()
                .id(3L).fullName("Admin User").email("admin@test.com")
                .role(Role.ADMIN).provider(AuthProvider.LOCAL).active(true)
                .createdAt(now).updatedAt(now).build();
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {
        @Test
        @DisplayName("should return paginated users with no filters")
        void listUsersNoFilters() {
            Page<User> page = new PageImpl<>(List.of(driver, manager), PageRequest.of(0, 10), 2);
            when(userRepository.searchUsers(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

            UserPageResponse response = adminService.listUsers(null, null, 0, 10);

            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getTotalElements()).isEqualTo(2);
            assertThat(response.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("should filter by role")
        void listUsersByRole() {
            Page<User> page = new PageImpl<>(List.of(driver), PageRequest.of(0, 10), 1);
            when(userRepository.searchUsers(isNull(), eq(Role.DRIVER), any(Pageable.class))).thenReturn(page);

            UserPageResponse response = adminService.listUsers(null, Role.DRIVER, 0, 10);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getRole()).isEqualTo(Role.DRIVER);
        }

        @Test
        @DisplayName("should treat blank search as null")
        void listUsersBlankSearch() {
            Page<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(userRepository.searchUsers(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

            adminService.listUsers("   ", null, 0, 10);

            verify(userRepository).searchUsers(isNull(), isNull(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {
        @Test
        @DisplayName("should return user when found")
        void getUserFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));

            UserResponse response = adminService.getUserById(1L);

            assertThat(response.getEmail()).isEqualTo("john@test.com");
            assertThat(response.getFullName()).isEqualTo("John Driver");
        }

        @Test
        @DisplayName("should throw when user not found")
        void getUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUserRole")
    class UpdateUserRole {
        @Test
        @DisplayName("should update role successfully")
        void updateRole() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            AdminUpdateUserRequest request = new AdminUpdateUserRequest();
            request.setRole(Role.MANAGER);
            UserResponse response = adminService.updateUserRole(1L, request);

            assertThat(response.getRole()).isEqualTo(Role.MANAGER);
            verify(userRepository).save(driver);
        }
    }

    @Nested
    @DisplayName("suspendUser")
    class SuspendUser {
        @Test
        @DisplayName("should suspend active non-admin user")
        void suspendSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = adminService.suspendUser(1L);

            assertThat(response).isNotNull();
            verify(userRepository).save(driver);
            assertThat(driver.isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw when suspending admin")
        void suspendAdmin() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> adminService.suspendUser(3L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("admin");
        }

        @Test
        @DisplayName("should throw when user already suspended")
        void suspendAlreadySuspended() {
            driver.setActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> adminService.suspendUser(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already suspended");
        }
    }

    @Nested
    @DisplayName("reactivateUser")
    class ReactivateUser {
        @Test
        @DisplayName("should reactivate suspended user")
        void reactivateSuccess() {
            driver.setActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            adminService.reactivateUser(1L);

            assertThat(driver.isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw when user already active")
        void reactivateAlreadyActive() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> adminService.reactivateUser(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already active");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {
        @Test
        @DisplayName("should delete non-admin user and their OTPs")
        void deleteSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(driver));

            adminService.deleteUser(1L);

            verify(otpRepository).deleteAllByEmailIgnoreCase("john@test.com");
            verify(userRepository).deleteUserById(1L);
        }

        @Test
        @DisplayName("should throw when deleting admin")
        void deleteAdmin() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> adminService.deleteUser(3L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("admin");
        }
    }

    @Nested
    @DisplayName("getUserStats")
    class GetUserStats {
        @Test
        @DisplayName("should return correct stats")
        void statsReturned() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByRole(Role.DRIVER)).thenReturn(70L);
            when(userRepository.countByRole(Role.MANAGER)).thenReturn(25L);
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(5L);
            when(userRepository.countByActive(true)).thenReturn(90L);
            when(userRepository.countByActive(false)).thenReturn(10L);

            UserStatsResponse stats = adminService.getUserStats();

            assertThat(stats.getTotalUsers()).isEqualTo(100);
            assertThat(stats.getTotalDrivers()).isEqualTo(70);
            assertThat(stats.getTotalManagers()).isEqualTo(25);
            assertThat(stats.getTotalAdmins()).isEqualTo(5);
            assertThat(stats.getActiveUsers()).isEqualTo(90);
            assertThat(stats.getInactiveUsers()).isEqualTo(10);
        }
    }
}
