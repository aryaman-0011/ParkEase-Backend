package com.parkease.auth.controller;

import com.parkease.auth.dto.ApiMessageResponse;
import com.parkease.auth.dto.AuthResponse;
import com.parkease.auth.dto.ChangePasswordRequest;
import com.parkease.auth.dto.ForgotPasswordRequest;
import com.parkease.auth.dto.LoginRequest;
import com.parkease.auth.dto.RegisterRequest;
import com.parkease.auth.dto.ResetPasswordRequest;
import com.parkease.auth.dto.UpdateProfileRequest;
import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.dto.VerifyOtpRequest;
import com.parkease.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Handles all authentication-related endpoints — register, login, password reset, profile, OAuth2
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Register a new user and return JWT tokens
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Authenticate user with email/password and return JWT tokens
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Issue a new access token using a valid refresh token from the Authorization header
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "").trim();
        return ResponseEntity.ok(authService.refreshToken(token));
    }

    // Get the currently authenticated user's profile
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    // Update authenticated user's profile (name, phone, vehicle plate, etc.)
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    // Change password for the authenticated user (requires old password)
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMessageResponse> changePassword(Authentication authentication,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new ApiMessageResponse("Password updated successfully"));
    }

    // Soft-deactivate the authenticated user's account
    @PutMapping("/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMessageResponse> deactivate(Authentication authentication) {
        authService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok(new ApiMessageResponse("Account deactivated successfully"));
    }

    // Permanently delete the authenticated user's account and all associated data
    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMessageResponse> deleteAccount(Authentication authentication) {
        authService.deleteAccount(authentication.getName());
        return ResponseEntity.ok(new ApiMessageResponse("Account and all associated data deleted successfully"));
    }

    // Step 1 of password reset — sends a 6-digit OTP to the user's email
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new ApiMessageResponse("OTP sent to your email address"));
    }

    // Step 2 of password reset — verifies the OTP entered by the user
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiMessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(new ApiMessageResponse("OTP verified successfully"));
    }

    // Step 3 of password reset — sets a new password after OTP verification
    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new ApiMessageResponse("Password reset successfully. You can now login with your new password."));
    }

    // Redirects the user to Google's OAuth2 consent page
    @GetMapping("/oauth2/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    // Client-side logout — instructs frontend to drop the JWT token
    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout() {
        return ResponseEntity.ok(new ApiMessageResponse("Logout successful on client side. Drop the JWT token or cookie."));
    }
}
