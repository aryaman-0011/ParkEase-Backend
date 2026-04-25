package com.parkease.vehicle_service.exception;

/**
 * Custom exception for missing resources (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
