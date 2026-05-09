package com.parkease.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import javax.crypto.SecretKey;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private static final String SECRET = "U29tZVN1cGVyU2VjdXJlQmFzZTY0RW5jb2RlZEtleUZvclBhcmsxMjM0NTY3ODkwMTIzNDU2Nzg5MA==";
    private JwtUtil jwtUtil;
    private SecretKey key;

    @BeforeEach void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private String generateToken(long userId, String role, String email, long expiryMs) {
        return Jwts.builder().claims(Map.of("userId", (int) userId, "role", role))
                .subject(email).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key).compact();
    }

    @Test void validToken() {
        String token = generateToken(42, "ADMIN", "a@b.com", 60000);
        Claims c = jwtUtil.validateAndGetClaims(token);
        assertNotNull(c);
        assertEquals("a@b.com", c.getSubject());
        assertEquals(42, c.get("userId", Integer.class));
        assertEquals("ADMIN", c.get("role", String.class));
    }

    @Test void expiredToken() {
        String token = generateToken(1, "DRIVER", "x@y.com", -1000);
        assertNull(jwtUtil.validateAndGetClaims(token));
    }

    @Test void garbageToken() {
        assertNull(jwtUtil.validateAndGetClaims("not.a.jwt"));
    }

    @Test void emptyToken() {
        assertNull(jwtUtil.validateAndGetClaims(""));
    }
}
