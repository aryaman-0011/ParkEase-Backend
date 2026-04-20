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
    @Size(max = 120)
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+\\- ]{10,20}$", message = "Phone number is invalid")
    private String phone;

    @Size(max = 500)
    private String profilePicUrl;

    @Size(max = 20)
    private String vehiclePlate;
}
