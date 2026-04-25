package com.parkease.booking_service.exception;

/**
 * Custom exception for missing resources (HTTP 404).
 * Thrown when a booking ID is not found in the database.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
