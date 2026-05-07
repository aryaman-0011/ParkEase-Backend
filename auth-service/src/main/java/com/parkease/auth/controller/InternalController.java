package com.parkease.auth.controller;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.Role;
import com.parkease.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Internal endpoint for service-to-service communication.
// Not exposed through the API gateway — called directly by other services.
@Slf4j
@RestController
@RequestMapping("/auth/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;

    // Returns all admin user IDs — used by parkinglot-service to notify admins
    @GetMapping("/admin-ids")
    public ResponseEntity<Map<String, List<Long>>> getAdminIds() {
        List<Long> ids = userRepository.findByRole(Role.ADMIN, Pageable.unpaged())
                .map(User::getId)
                .toList();
        return ResponseEntity.ok(Map.of("ids", ids));
    }

    // Returns user IDs by role — used by notification-service for admin broadcasts
    @GetMapping("/user-ids-by-role")
    public ResponseEntity<Map<String, List<Long>>> getUserIdsByRole(@org.springframework.web.bind.annotation.RequestParam Role role) {
        List<Long> ids = userRepository.findByRole(role, Pageable.unpaged())
                .map(User::getId)
                .toList();
        return ResponseEntity.ok(Map.of("ids", ids));
    }
}
