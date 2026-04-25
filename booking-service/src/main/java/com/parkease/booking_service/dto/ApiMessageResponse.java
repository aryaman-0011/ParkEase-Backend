package com.parkease.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic API response DTO containing a simple message string.
 * Used for success/error responses that do not return domain data.
 */
@Data
@AllArgsConstructor
public class ApiMessageResponse {
    private String message;
}
