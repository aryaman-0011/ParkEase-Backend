package com.parkease.booking_service.exception;

/**
 * Custom exception for invalid client requests (HTTP 400).
 * Thrown when booking validation fails (e.g., past time, conflicts).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
