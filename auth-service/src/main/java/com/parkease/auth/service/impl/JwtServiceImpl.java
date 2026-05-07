package com.parkease.auth.service.impl;

import com.parkease.auth.entity.User;
import com.parkease.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;
    private final long rememberMeExpirationMillis;

    public JwtServiceImpl(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMillis,
            @Value("${app.jwt.remember-me-expiration-ms:604800000}") long rememberMeExpirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMillis = expirationMillis;
        this.rememberMeExpirationMillis = rememberMeExpirationMillis;
    }

    @Override
    public String generateToken(User user) {
        return generateToken(user, false);
    }

    @Override
    public String generateToken(User user, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("provider", user.getProvider().name());

        long effectiveExpiration = rememberMe ? rememberMeExpirationMillis : expirationMillis;

        Date now = new Date();
        Date expiry = new Date(now.getTime() + effectiveExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }


    @Override
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(String token, String username) {
        Claims claims = getClaims(token);
        return username.equalsIgnoreCase(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    @Override
    public long getExpirationInSeconds() {
        return expirationMillis / 1000;
    }

    @Override
    public long getRememberMeExpirationInSeconds() {
        return rememberMeExpirationMillis / 1000;
    }

    @Override
    public long getRemainingMillis(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
