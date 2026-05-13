package com.parkease.auth.service.impl;

import com.parkease.auth.client.ParkingLotServiceClient;
import com.parkease.auth.dto.AuthResponse;
import com.parkease.auth.dto.ChangePasswordRequest;
import com.parkease.auth.dto.ForgotPasswordRequest;
import com.parkease.auth.dto.LoginRequest;
import com.parkease.auth.dto.RegisterRequest;
import com.parkease.auth.dto.ResetPasswordRequest;
import com.parkease.auth.dto.UpdateProfileRequest;
import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.dto.VerifyOtpRequest;
import com.parkease.auth.entity.PasswordResetOtp;
import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.exception.BadRequestException;
import com.parkease.auth.exception.ResourceNotFoundException;
import com.parkease.auth.exception.UnauthorizedException;
import com.parkease.auth.repository.PasswordResetOtpRepository;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.service.EmailService;
import com.parkease.auth.service.JwtService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final RedisOtpService redisOtpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final ParkingLotServiceClient parkingLotServiceClient;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String USER_NOT_FOUND = "User not found";

    @Value("${app.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role role = request.getRole() == null ? Role.DRIVER : request.getRole();

        // Manager accounts require business details
        if (role == Role.MANAGER) {
            if (request.getBusinessName() == null || request.getBusinessName().isBlank()) {
                throw new BadRequestException("Business name is required for manager accounts");
            }
            if (request.getBusinessRegistration() == null || request.getBusinessRegistration().isBlank()) {
                throw new BadRequestException("Business registration number is required for manager accounts");
            }
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .vehiclePlate(request.getVehiclePlate())
                .businessName(request.getBusinessName())
                .businessRegistration(request.getBusinessRegistration())
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: email={} role={}", savedUser.getEmail(), savedUser.getRole());
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());
        return toAuthResponse(savedUser, false);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getEmail().trim().toLowerCase(), request.getPassword()));
            String email = authentication.getName();
            User user = getActiveUserByEmail(email);
            emailService.sendLoginNotificationEmail(user.getEmail(), user.getFullName(), "Email & Password");
            return toAuthResponse(user, request.isRememberMe());
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(getActiveUserByEmail(email));
    }

    @Override
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getActiveUserByEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        user.setProfilePicUrl(request.getProfilePicUrl());
        user.setVehiclePlate(request.getVehiclePlate());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getActiveUserByEmail(email);
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password cannot be changed for social-login accounts");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void deactivateAccount(String email) {
        User user = getActiveUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public void deleteAccount(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be deleted through self-service");
        }

        // If the user is a MANAGER, delete their parking lots from the parkinglot-service
        if (user.getRole() == Role.MANAGER) {
            try {
                parkingLotServiceClient.deleteAllLotsByManager(user.getId());
                log.info("Deleted all parking lots for manager id={}", user.getId());
            } catch (Exception ex) {
                log.warn("Failed to delete parking lots for manager id={}: {}", user.getId(), ex.getMessage());
                // Continue with account deletion even if lot cleanup fails
            }
        }

        // Delete all OTPs associated with this user (Redis)
        redisOtpService.deleteAllForEmail(user.getEmail());

        // Delete the user record
        userRepository.deleteUserById(user.getId());
        log.info("Account deleted for user email={}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String currentToken) {
        try {
            String email = jwtService.extractUsername(currentToken);
            User user = getActiveUserByEmail(email);
            if (!jwtService.isTokenValid(currentToken, email)) {
                throw new UnauthorizedException("JWT token is invalid or expired");
            }
            return toAuthResponse(user, false);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("JWT token is invalid or expired");
        }
    }

    @Override
    public User upsertOAuthUser(AuthProvider provider, String providerId, String email, String fullName, String imageUrl, Role defaultRole) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new BadRequestException("OAuth provider did not supply a usable email address");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .role(defaultRole == null ? Role.DRIVER : defaultRole)
                        .provider(provider)
                        .active(true)
                        .build());

        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setFullName(fullName == null || fullName.isBlank() ? normalizedEmail : fullName);
        user.setProfilePicUrl(imageUrl);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is not available for social-login accounts. Please use " + user.getProvider().name() + " to login.");
        }

        // Rate limiting: check if an OTP was sent within the last 60 seconds (Redis TTL)
        if (redisOtpService.isRateLimited(email)) {
            throw new BadRequestException("Please wait 60 seconds before requesting a new OTP");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        // Store in Redis with TTL-based expiration (auto-deletes after expiry)
        redisOtpService.storeOtp(email, otp, otpExpirationMinutes);

        // Send OTP email
        emailService.sendOtpEmail(email, otp);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!redisOtpService.validateOtp(email, request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!redisOtpService.validateOtp(email, request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Consume the OTP so it can't be reused
        redisOtpService.invalidateOtp(email);
    }

    private User getActiveUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new UnauthorizedException("User account is deactivated");
        }
        return user;
    }

    private AuthResponse toAuthResponse(User user, boolean rememberMe) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user, rememberMe))
                .tokenType("Bearer")
                .expiresIn(rememberMe ? jwtService.getRememberMeExpirationInSeconds() : jwtService.getExpirationInSeconds())
                .user(UserResponse.from(user))
                .build();
    }
}
