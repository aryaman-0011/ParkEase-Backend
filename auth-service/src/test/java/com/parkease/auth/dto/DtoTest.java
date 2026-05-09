package com.parkease.auth.dto;

import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {
    @Test void authResponse() {
        var user = UserResponse.builder().id(1L).email("e").fullName("n").role(Role.DRIVER).active(true).build();
        var r = AuthResponse.builder().accessToken("a").tokenType("Bearer").expiresIn(3600).user(user).build();
        assertEquals("a", r.getAccessToken());
        assertEquals("Bearer", r.getTokenType());
        assertEquals(1L, r.getUser().getId());
    }
    @Test void userResponse() {
        var r = UserResponse.builder().id(1L).email("e").fullName("n").role(Role.ADMIN)
                .active(true).provider(AuthProvider.LOCAL).build();
        assertEquals(1L, r.getId());
        assertTrue(r.isActive());
        assertEquals(Role.ADMIN, r.getRole());
    }
    @Test void apiMessageResponse() {
        var r = new ApiMessageResponse("msg");
        assertEquals("msg", r.getMessage());
    }
    @Test void loginRequest() {
        var r = new LoginRequest();
        r.setEmail("e"); r.setPassword("p");
        assertEquals("e", r.getEmail());
    }
    @Test void registerRequest() {
        var r = new RegisterRequest();
        r.setEmail("e"); r.setFullName("n"); r.setPassword("p");
        assertEquals("n", r.getFullName());
    }
    @Test void userPageResponse() {
        var r = new UserPageResponse();
        r.setContent(List.of());
        r.setTotalElements(100L);
        r.setTotalPages(10);
        r.setPage(0);
        r.setSize(10);
        r.setLast(false);
        assertEquals(100L, r.getTotalElements());
        assertEquals(0, r.getPage());
    }
    @Test void userStatsResponse() {
        var r = new UserStatsResponse();
        r.setTotalUsers(10L);
        r.setActiveUsers(8L);
        r.setInactiveUsers(2L);
        r.setTotalDrivers(5L);
        r.setTotalManagers(3L);
        r.setTotalAdmins(2L);
        assertEquals(10L, r.getTotalUsers());
    }
}
