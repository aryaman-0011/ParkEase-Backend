package com.parkease.auth.dto;

import com.parkease.auth.enums.Role;
import jakarta.validation.constraints.Email;
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
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 50, message = "Full name must be 2-50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z\\s]{0,48}[A-Za-z]$", message = "Full name must contain only letters and spaces")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
            message = "Password must include uppercase, lowercase, number, and special character")
    private String password;

    @Pattern(regexp = "^$|^(\\+91[\\-\\s]?)?[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian phone number")
    private String phone;

    private Role role;

    @Pattern(regexp = "^$|^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Enter a valid Indian vehicle plate (e.g. MH01AB1234)")
    @Size(max = 20)
    private String vehiclePlate;

    @Size(min = 2, max = 150, message = "Business name must be 2-150 characters")
    private String businessName;

    @Pattern(regexp = "^$|^\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z\\d][Z][A-Z\\d]$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Enter a valid 15-digit GST number")
    @Size(max = 50)
    private String businessRegistration;
}
