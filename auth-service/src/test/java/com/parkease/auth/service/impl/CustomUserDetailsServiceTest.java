package com.parkease.auth.service.impl;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).fullName("Test User").email("test@parkease.com")
                .passwordHash("encodedPass").role(Role.DRIVER)
                .provider(AuthProvider.LOCAL).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should load user details for existing user")
    void loadSuccess() {
        when(userRepository.findByEmailIgnoreCase("test@parkease.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("test@parkease.com");

        assertThat(details.getUsername()).isEqualTo("test@parkease.com");
        assertThat(details.getPassword()).isEqualTo("encodedPass");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_DRIVER");
    }

    @Test
    @DisplayName("should set disabled=true for inactive user")
    void loadInactive() {
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("test@parkease.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("test@parkease.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should handle null password for OAuth users")
    void loadOAuthUser() {
        user.setPasswordHash(null);
        user.setProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmailIgnoreCase("test@parkease.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("test@parkease.com");

        assertThat(details.getPassword()).isEmpty();
    }

    @Test
    @DisplayName("should set ROLE_MANAGER authority for managers")
    void loadManager() {
        user.setRole(Role.MANAGER);
        when(userRepository.findByEmailIgnoreCase("test@parkease.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("test@parkease.com");

        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_MANAGER");
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user not found")
    void loadNotFound() {
        when(userRepository.findByEmailIgnoreCase("unknown@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
