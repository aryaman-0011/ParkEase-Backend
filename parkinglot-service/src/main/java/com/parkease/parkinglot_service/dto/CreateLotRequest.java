package com.parkease.parkinglot_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a new parking lot.
 * Contains name, address, city, capacity, pricing, and manager info.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLotRequest {

    @NotBlank(message = "Lot name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\s\\-.,()]{0,148}$", message = "Lot name contains invalid characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 300, message = "Address must be 5-300 characters")
    @Pattern(regexp = "^[A-Za-z0-9#][A-Za-z0-9\\s,.\\-/#()]{3,298}$", message = "Address contains invalid characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be 2-100 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z\\s\\-]{0,98}$", message = "City must contain only letters, spaces, and hyphens")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Zip code must not exceed 20 characters")
    private String zipCode;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @Min(value = 1, message = "Total spots must be at least 1")
    private int totalSpots;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
