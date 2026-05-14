package com.parkease.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 50, message = "Full name must be 2-50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z\\s]{0,48}[A-Za-z]$", message = "Full name must contain only letters and spaces")
    private String fullName;

    @Pattern(regexp = "^$|^(\\+91[\\-\\s]?)?[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian phone number")
    private String phone;

    @Size(max = 500)
    private String profilePicUrl;

    @Pattern(regexp = "^$|^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Enter a valid Indian vehicle plate (e.g. MH01AB1234)")
    @Size(max = 20)
    private String vehiclePlate;
}
