package com.parkease.parkinglot_service.exception;

/**
 * Custom exception for missing resources (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
