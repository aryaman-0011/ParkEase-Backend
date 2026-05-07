package com.parkease.auth.service.impl;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;
    private User user;

    // Base64-encoded 256-bit key for testing
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTI1Ng==";
    private static final long EXPIRATION_MS = 3600000; // 1 hour
    private static final long REMEMBER_ME_MS = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(TEST_SECRET, EXPIRATION_MS, REMEMBER_ME_MS);
        user = User.builder()
                .id(1L).fullName("Test User").email("test@parkease.com")
                .role(Role.DRIVER).provider(AuthProvider.LOCAL).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {
        @Test
        @DisplayName("should generate a valid JWT token")
        void generateValid() {
            String token = jwtService.generateToken(user);

            assertThat(token).isNotBlank();
            // Token should have 3 parts (header.payload.signature)
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("should generate token with user email as subject")
        void tokenSubject() {
            String token = jwtService.generateToken(user);
            String subject = jwtService.extractUsername(token);

            assertThat(subject).isEqualTo("test@parkease.com");
        }

        @Test
        @DisplayName("should include userId and role claims")
        void tokenClaims() {
            String token = jwtService.generateToken(user);

            var claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
            assertThat(claims.get("provider", String.class)).isEqualTo("LOCAL");
        }

        @Test
        @DisplayName("should generate different tokens for rememberMe")
        void rememberMeToken() {
            String normalToken = jwtService.generateToken(user, false);
            String rememberToken = jwtService.generateToken(user, true);

            // Both should be valid but have different expiration
            assertThat(normalToken).isNotEqualTo(rememberToken);
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {
        @Test
        @DisplayName("should extract email from token")
        void extractEmail() {
            String token = jwtService.generateToken(user);

            assertThat(jwtService.extractUsername(token)).isEqualTo("test@parkease.com");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {
        @Test
        @DisplayName("should return true for valid token and matching username")
        void validToken() {
            String token = jwtService.generateToken(user);

            assertThat(jwtService.isTokenValid(token, "test@parkease.com")).isTrue();
        }

        @Test
        @DisplayName("should return false for mismatched username")
        void mismatchedUsername() {
            String token = jwtService.generateToken(user);

            assertThat(jwtService.isTokenValid(token, "other@parkease.com")).isFalse();
        }

        @Test
        @DisplayName("should return false for expired token")
        void expiredToken() {
            // Create service with 0ms expiration
            JwtServiceImpl expiredService = new JwtServiceImpl(TEST_SECRET, 0, 0);
            String token = expiredService.generateToken(user);

            // Token is already expired
            assertThatThrownBy(() -> expiredService.isTokenValid(token, "test@parkease.com"))
                    .isInstanceOf(Exception.class); // ExpiredJwtException
        }
    }

    @Nested
    @DisplayName("getExpirationInSeconds")
    class ExpirationSeconds {
        @Test
        @DisplayName("should return expiration in seconds")
        void normalExpiration() {
            assertThat(jwtService.getExpirationInSeconds()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("should return remember-me expiration in seconds")
        void rememberMeExpiration() {
            assertThat(jwtService.getRememberMeExpirationInSeconds()).isEqualTo(604800L);
        }
    }
}
