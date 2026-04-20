package com.parkease.auth.service;

import com.parkease.auth.dto.AdminUpdateUserRequest;
import com.parkease.auth.dto.UserPageResponse;
import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.dto.UserStatsResponse;
import com.parkease.auth.enums.Role;

public interface AdminService {

    UserPageResponse listUsers(String search, Role role, int page, int size);

    UserResponse getUserById(Long id);

    UserResponse updateUserRole(Long id, AdminUpdateUserRequest request);

    UserResponse suspendUser(Long id);

    UserResponse reactivateUser(Long id);

    void deleteUser(Long id);

    UserStatsResponse getUserStats();
}
