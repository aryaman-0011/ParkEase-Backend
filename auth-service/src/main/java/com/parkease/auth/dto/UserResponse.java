package com.parkease.auth.dto;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import java.time.LocalDateTime;
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
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String vehiclePlate;
    private AuthProvider provider;
    private boolean active;
    private String profilePicUrl;
    private String businessName;
    private String businessRegistration;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .vehiclePlate(user.getVehiclePlate())
                .provider(user.getProvider())
                .active(user.isActive())
                .profilePicUrl(user.getProfilePicUrl())
                .businessName(user.getBusinessName())
                .businessRegistration(user.getBusinessRegistration())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
