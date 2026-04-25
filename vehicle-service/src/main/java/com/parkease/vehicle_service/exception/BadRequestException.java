package com.parkease.vehicle_service.exception;

/**
 * Custom exception for invalid client requests (HTTP 400).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
