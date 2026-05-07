package com.parkease.auth.security;

import com.parkease.auth.service.JwtService;
import com.parkease.auth.service.impl.CustomUserDetailsService;
import com.parkease.auth.service.impl.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        // Default: no tokens are blacklisted
        lenient().when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("should continue without auth when no Authorization header")
    void noHeader() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should continue without auth when header is not Bearer")
    void nonBearerHeader() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should set authentication for valid Bearer token")
    void validToken() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("user@test.com");

        UserDetails userDetails = User.builder()
                .username("user@test.com").password("pass")
                .authorities("ROLE_DRIVER").build();
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", "user@test.com")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("should skip auth when token is invalid")
    void invalidToken() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad-token");
        when(jwtService.extractUsername("bad-token")).thenReturn("user@test.com");

        UserDetails userDetails = User.builder()
                .username("user@test.com").password("pass")
                .authorities("ROLE_DRIVER").build();
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("bad-token", "user@test.com")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should skip auth when extractUsername throws")
    void malformedToken() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer malformed");
        when(jwtService.extractUsername("malformed")).thenThrow(new RuntimeException("Bad JWT"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should clear context when userDetailsService throws")
    void userNotFound() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("gone@test.com");
        when(userDetailsService.loadUserByUsername("gone@test.com"))
                .thenThrow(new RuntimeException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should skip auth when token is blacklisted (logged out)")
    void blacklistedToken() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer blacklisted-token");
        when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Should never attempt to extract username for a blacklisted token
        verify(jwtService, never()).extractUsername(anyString());
    }
}
