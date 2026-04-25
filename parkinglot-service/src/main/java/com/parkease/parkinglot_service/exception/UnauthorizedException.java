package com.parkease.parkinglot_service.exception;

/**
 * Custom exception for unauthorized access attempts (HTTP 403).
 * Thrown when a user tries to modify a lot they do not manage.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
