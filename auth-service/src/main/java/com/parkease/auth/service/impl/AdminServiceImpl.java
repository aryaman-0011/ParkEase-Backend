package com.parkease.auth.service.impl;

import com.parkease.auth.dto.AdminUpdateUserRequest;
import com.parkease.auth.dto.UserPageResponse;
import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.dto.UserStatsResponse;
import com.parkease.auth.entity.User;
import com.parkease.auth.enums.Role;
import com.parkease.auth.exception.BadRequestException;
import com.parkease.auth.exception.ResourceNotFoundException;
import com.parkease.auth.repository.PasswordResetOtpRepository;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPageResponse listUsers(String search, Role role, int page, int size) {
        log.info("Admin listing users: search={}, role={}, page={}, size={}", search, role, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.searchUsers(
                (search != null && search.isBlank()) ? null : search,
                role,
                pageable);

        return UserPageResponse.builder()
                .content(userPage.getContent().stream().map(UserResponse::from).toList())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.from(user);
    }

    @Override
    public UserResponse updateUserRole(Long id, AdminUpdateUserRequest request) {
        log.info("Admin updating role for userId={} to {}", id, request.getRole());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(request.getRole());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse suspendUser(Long id) {
        log.info("Admin suspending userId={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot suspend an admin account");
        }
        if (!user.isActive()) {
            throw new BadRequestException("User is already suspended");
        }
        user.setActive(false);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse reactivateUser(Long id) {
        log.info("Admin reactivating userId={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.isActive()) {
            throw new BadRequestException("User is already active");
        }
        user.setActive(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        log.warn("Admin deleting userId={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot delete an admin account");
        }
        otpRepository.deleteAllByEmailIgnoreCase(user.getEmail());
        userRepository.deleteUserById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        return UserStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalDrivers(userRepository.countByRole(Role.DRIVER))
                .totalManagers(userRepository.countByRole(Role.MANAGER))
                .totalAdmins(userRepository.countByRole(Role.ADMIN))
                .activeUsers(userRepository.countByActive(true))
                .inactiveUsers(userRepository.countByActive(false))
                .build();
    }
}
