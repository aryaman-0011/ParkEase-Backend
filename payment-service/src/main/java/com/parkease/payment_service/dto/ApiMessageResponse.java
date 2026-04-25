package com.parkease.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic API response DTO containing a simple message string.
 */
@Data
@AllArgsConstructor
public class ApiMessageResponse {
    private String message;
}
