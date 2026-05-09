package com.parkease.auth.entity;

import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @Test void builderAndGetters() {
        var u = User.builder().id(1L).email("a@b.com").fullName("Test").passwordHash("hash")
                .role(Role.DRIVER).provider(AuthProvider.LOCAL).phone("1234567890")
                .active(true).build();
        assertEquals(1L, u.getId());
        assertEquals("a@b.com", u.getEmail());
        assertEquals(Role.DRIVER, u.getRole());
        assertTrue(u.isActive());
        assertEquals("Test", u.getFullName());
    }
    @Test void prePersist() {
        var u = new User();
        u.onCreate();
        assertNotNull(u.getCreatedAt());
        assertNotNull(u.getUpdatedAt());
    }
    @Test void preUpdate() {
        var u = new User();
        u.onUpdate();
        assertNotNull(u.getUpdatedAt());
    }
    @Test void setters() {
        var u = new User();
        u.setEmail("x@y.com");
        u.setRole(Role.ADMIN);
        u.setActive(false);
        u.setFullName("New Name");
        assertEquals("x@y.com", u.getEmail());
        assertEquals(Role.ADMIN, u.getRole());
        assertFalse(u.isActive());
    }
}
