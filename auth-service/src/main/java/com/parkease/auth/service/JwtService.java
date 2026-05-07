package com.parkease.auth.service;

import com.parkease.auth.entity.User;

public interface JwtService {

    String generateToken(User user);

    String generateToken(User user, boolean rememberMe);

    String extractUsername(String token);

    boolean isTokenValid(String token, String username);

    long getExpirationInSeconds();

    long getRememberMeExpirationInSeconds();

    long getRemainingMillis(String token);
}
