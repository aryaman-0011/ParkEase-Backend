package com.parkease.auth.controller;

import com.parkease.auth.dto.*;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    @Mock private AdminService adminService;
    @InjectMocks private AdminController controller;

    private UserResponse userSample() {
        return UserResponse.builder().id(1L).email("a@b.com").fullName("Test")
                .role(Role.DRIVER).active(true).provider(AuthProvider.LOCAL).build();
    }

    @Test void listUsers() {
        var page = new UserPageResponse();
        page.setContent(List.of(userSample()));
        page.setTotalElements(1L);
        page.setTotalPages(1);
        page.setPage(0);
        page.setSize(10);
        page.setLast(true);
        when(adminService.listUsers(null, null, 0, 10)).thenReturn(page);
        assertEquals(HttpStatus.OK, controller.listUsers(null, null, 0, 10).getStatusCode());
    }
    @Test void getUserById() {
        when(adminService.getUserById(1L)).thenReturn(userSample());
        assertEquals(HttpStatus.OK, controller.getUserById(1L).getStatusCode());
    }
    @Test void updateUserRole() {
        when(adminService.updateUserRole(eq(1L), any())).thenReturn(userSample());
        assertEquals(HttpStatus.OK, controller.updateUserRole(1L, new AdminUpdateUserRequest()).getStatusCode());
    }
    @Test void suspendUser() {
        when(adminService.suspendUser(1L)).thenReturn(userSample());
        assertEquals(HttpStatus.OK, controller.suspendUser(1L).getStatusCode());
    }
    @Test void reactivateUser() {
        when(adminService.reactivateUser(1L)).thenReturn(userSample());
        assertEquals(HttpStatus.OK, controller.reactivateUser(1L).getStatusCode());
    }
    @Test void deleteUser() {
        doNothing().when(adminService).deleteUser(1L);
        assertEquals(HttpStatus.OK, controller.deleteUser(1L).getStatusCode());
    }
    @Test void getUserStats() {
        var stats = new UserStatsResponse();
        stats.setTotalUsers(10L);
        stats.setActiveUsers(8L);
        stats.setInactiveUsers(2L);
        stats.setTotalDrivers(5L);
        stats.setTotalManagers(3L);
        stats.setTotalAdmins(2L);
        when(adminService.getUserStats()).thenReturn(stats);
        assertEquals(HttpStatus.OK, controller.getUserStats().getStatusCode());
    }
}
