package com.parkease.auth.controller;

import com.parkease.auth.dto.AdminUpdateUserRequest;
import com.parkease.auth.dto.ApiMessageResponse;
import com.parkease.auth.dto.UserPageResponse;
import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.dto.UserStatsResponse;
import com.parkease.auth.enums.Role;
import com.parkease.auth.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Admin-only endpoints for user management — list, update roles, suspend, delete users
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // All endpoints here require ADMIN role
public class AdminController {

    private final AdminService adminService;

    // List users with optional search, role filter, and pagination
    @GetMapping("/users")
    public ResponseEntity<UserPageResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.listUsers(search, role, page, size));
    }

    // Get a specific user by their ID
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    // Update a user's role (e.g. DRIVER -> MANAGER or ADMIN)
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request));
    }

    // Suspend a user account — prevents them from logging in
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<UserResponse> suspendUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.suspendUser(id));
    }

    // Reactivate a previously suspended or deactivated user
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> reactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.reactivateUser(id));
    }

    // Permanently delete a user account
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiMessageResponse> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(new ApiMessageResponse("User deleted successfully"));
    }

    // Get dashboard stats — total users, active, suspended, by role counts
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats() {
        return ResponseEntity.ok(adminService.getUserStats());
    }
}
