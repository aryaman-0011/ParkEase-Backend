package com.parkease.auth.service.impl;

import com.parkease.auth.client.ParkingLotServiceClient;
import com.parkease.auth.dto.*;
import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.exception.BadRequestException;
import com.parkease.auth.exception.ResourceNotFoundException;
import com.parkease.auth.exception.UnauthorizedException;
import com.parkease.auth.repository.PasswordResetOtpRepository;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.EmailService;
import com.parkease.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetOtpRepository otpRepository;
    @Mock private RedisOtpService redisOtpService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private ParkingLotServiceClient parkingLotServiceClient;
    @InjectMocks private AuthServiceImpl authService;

    private User driver;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        driver = User.builder()
                .id(1L).fullName("John Driver").email("john@test.com")
                .passwordHash("encodedPassword").phone("1234567890")
                .role(Role.DRIVER).provider(AuthProvider.LOCAL).active(true)
                .createdAt(now).updatedAt(now).build();
        manager = User.builder()
                .id(2L).fullName("Jane Manager").email("jane@test.com")
                .passwordHash("encodedPassword")
                .role(Role.MANAGER).provider(AuthProvider.LOCAL).active(true)
                .businessName("ParkCo").businessRegistration("REG123")
                .createdAt(now).updatedAt(now).build();
        admin = User.builder()
                .id(3L).fullName("Admin User").email("admin@test.com")
                .passwordHash("encodedPassword")
                .role(Role.ADMIN).provider(AuthProvider.LOCAL).active(true)
                .createdAt(now).updatedAt(now).build();
    }

    @Nested
    @DisplayName("register")
    class Register {
        @Test
        @DisplayName("should register a new driver successfully")
        void registerDriver() {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName("New User").email("new@test.com")
                    .password("password123").role(Role.DRIVER).build();

            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(10L);
                u.setCreatedAt(LocalDateTime.now());
                u.setUpdatedAt(LocalDateTime.now());
                return u;
            });
            when(jwtService.generateToken(any(User.class), eq(false))).thenReturn("jwt-token");
            when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse response = authService.register(req);

            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getUser().getEmail()).isEqualTo("new@test.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw when email already exists")
        void registerDuplicateEmail() {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName("Dup").email("john@test.com").password("pass1234").build();

            when(userRepository.existsByEmailIgnoreCase("john@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should require business name for manager registration")
        void registerManagerNoBusinessName() {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName("Mgr").email("mgr@test.com").password("pass1234")
                    .role(Role.MANAGER).businessRegistration("REG1").build();

            when(userRepository.existsByEmailIgnoreCase("mgr@test.com")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Business name");
        }

        @Test
        @DisplayName("should require business registration for manager")
        void registerManagerNoRegistration() {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName("Mgr").email("mgr@test.com").password("pass1234")
                    .role(Role.MANAGER).businessName("ParkCo").build();

            when(userRepository.existsByEmailIgnoreCase("mgr@test.com")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Business registration");
        }

        @Test
        @DisplayName("should require non-blank business registration for manager")
        void registerManagerBlankRegistration() {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName("Mgr").email("mgr@test.com").password("pass1234")
                    .role(Role.MANAGER).businessName("ParkCo").businessRegistration("   ").build();

            when(userRepository.existsByEmailIgnoreCase("mgr@test.com")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Business registration");
        }

        @Test
        @DisplayName("should default role to DRIVER when null")
        void registerDefaultRole() {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName("User").email("user@test.com").password("pass1234")
                    .role(null).build();

            when(userRepository.existsByEmailIgnoreCase("user@test.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(11L);
                u.setCreatedAt(LocalDateTime.now());
                u.setUpdatedAt(LocalDateTime.now());
                return u;
            });
            when(jwtService.generateToken(any(User.class), eq(false))).thenReturn("token");
            when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse response = authService.register(req);

            assertThat(response.getUser().getRole()).isEqualTo(Role.DRIVER);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {
        @Test
        @DisplayName("should login with valid credentials")
        void loginSuccess() {
            LoginRequest req = LoginRequest.builder()
                    .email("john@test.com").password("password123").rememberMe(false).build();

            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("john@test.com");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(jwtService.generateToken(driver, false)).thenReturn("jwt-token");
            when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse response = authService.login(req);

            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            verify(emailService).sendLoginNotificationEmail(eq("john@test.com"), eq("John Driver"), anyString());
        }

        @Test
        @DisplayName("should use remember-me expiration when requested")
        void loginRememberMeSuccess() {
            LoginRequest req = LoginRequest.builder()
                    .email("john@test.com").password("password123").rememberMe(true).build();

            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("john@test.com");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(jwtService.generateToken(driver, true)).thenReturn("remember-token");
            when(jwtService.getRememberMeExpirationInSeconds()).thenReturn(604800L);

            AuthResponse response = authService.login(req);

            assertThat(response.getAccessToken()).isEqualTo("remember-token");
            assertThat(response.getExpiresIn()).isEqualTo(604800L);
        }

        @Test
        @DisplayName("should throw on invalid credentials")
        void loginInvalid() {
            LoginRequest req = LoginRequest.builder()
                    .email("john@test.com").password("wrong").build();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("should throw when user is deactivated")
        void loginDeactivated() {
            driver.setActive(false);
            LoginRequest req = LoginRequest.builder()
                    .email("john@test.com").password("password123").build();

            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("john@test.com");
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {
        @Test
        @DisplayName("should return user by email")
        void getUser() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));

            UserResponse response = authService.getCurrentUser("john@test.com");

            assertThat(response.getEmail()).isEqualTo("john@test.com");
        }

        @Test
        @DisplayName("should throw when user not found")
        void userNotFound() {
            when(userRepository.findByEmailIgnoreCase("x@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser("x@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {
        @Test
        @DisplayName("should update profile fields")
        void updateSuccess() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UpdateProfileRequest req = UpdateProfileRequest.builder()
                    .fullName("Updated Name").phone("9876543210").build();

            UserResponse response = authService.updateProfile("john@test.com", req);

            assertThat(response.getFullName()).isEqualTo("Updated Name");
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {
        @Test
        @DisplayName("should change password when current is correct")
        void changeSuccess() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(passwordEncoder.matches("oldpass", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.matches("newpass", "encodedPassword")).thenReturn(false);
            when(passwordEncoder.encode("newpass")).thenReturn("newEncoded");

            authService.changePassword("john@test.com",
                    ChangePasswordRequest.builder()
                            .currentPassword("oldpass").newPassword("newpass").build());

            verify(userRepository).save(driver);
            assertThat(driver.getPasswordHash()).isEqualTo("newEncoded");
        }

        @Test
        @DisplayName("should throw when current password is wrong")
        void changeWrongCurrent() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword("john@test.com",
                    ChangePasswordRequest.builder()
                            .currentPassword("wrong").newPassword("newpass").build()))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("incorrect");
        }

        @Test
        @DisplayName("should throw when new password equals current")
        void changeSamePassword() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(passwordEncoder.matches("samepass", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword("john@test.com",
                    ChangePasswordRequest.builder()
                            .currentPassword("samepass").newPassword("samepass").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("different");
        }

        @Test
        @DisplayName("should throw for social login accounts")
        void changeSocialAccount() {
            driver.setProvider(AuthProvider.GOOGLE);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> authService.changePassword("john@test.com",
                    ChangePasswordRequest.builder()
                            .currentPassword("pass").newPassword("newpass").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("social-login");
        }
    }

    @Nested
    @DisplayName("deactivateAccount")
    class DeactivateAccount {
        @Test
        @DisplayName("should deactivate active user")
        void deactivate() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));

            authService.deactivateAccount("john@test.com");

            assertThat(driver.isActive()).isFalse();
            verify(userRepository).save(driver);
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {
        @Test
        @DisplayName("should delete driver account and Redis OTPs")
        void deleteDriver() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));

            authService.deleteAccount("john@test.com");

            verify(redisOtpService).deleteAllForEmail("john@test.com");
            verify(userRepository).deleteUserById(1L);
        }

        @Test
        @DisplayName("should throw when deleting admin account")
        void deleteAdmin() {
            when(userRepository.findByEmailIgnoreCase("admin@test.com"))
                    .thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> authService.deleteAccount("admin@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Admin");
        }

        @Test
        @DisplayName("should delete manager lots before deleting manager account")
        void deleteManagerDeletesLots() {
            when(userRepository.findByEmailIgnoreCase("jane@test.com"))
                    .thenReturn(Optional.of(manager));

            authService.deleteAccount("jane@test.com");

            verify(parkingLotServiceClient).deleteAllLotsByManager(2L);
            verify(redisOtpService).deleteAllForEmail("jane@test.com");
            verify(userRepository).deleteUserById(2L);
        }

        @Test
        @DisplayName("should continue manager deletion when lot cleanup fails")
        void deleteManagerContinuesWhenLotCleanupFails() {
            when(userRepository.findByEmailIgnoreCase("jane@test.com"))
                    .thenReturn(Optional.of(manager));
            doThrow(new RuntimeException("parkinglot unavailable"))
                    .when(parkingLotServiceClient).deleteAllLotsByManager(2L);

            authService.deleteAccount("jane@test.com");

            verify(redisOtpService).deleteAllForEmail("jane@test.com");
            verify(userRepository).deleteUserById(2L);
        }

        @Test
        @DisplayName("should throw when deleting missing account")
        void deleteMissingAccount() {
            when(userRepository.findByEmailIgnoreCase("missing@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.deleteAccount("missing@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {
        @Test
        @DisplayName("should refresh valid token")
        void refreshValid() {
            when(jwtService.extractUsername("old-token")).thenReturn("john@test.com");
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(jwtService.isTokenValid("old-token", "john@test.com")).thenReturn(true);
            when(jwtService.generateToken(driver, false)).thenReturn("new-token");
            when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse response = authService.refreshToken("old-token");

            assertThat(response.getAccessToken()).isEqualTo("new-token");
        }

        @Test
        @DisplayName("should throw when token is invalid")
        void refreshInvalid() {
            when(jwtService.extractUsername("bad-token")).thenReturn("john@test.com");
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(jwtService.isTokenValid("bad-token", "john@test.com")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should wrap token parsing errors")
        void refreshMalformedToken() {
            when(jwtService.extractUsername("malformed-token"))
                    .thenThrow(new IllegalArgumentException("bad token"));

            assertThatThrownBy(() -> authService.refreshToken("malformed-token"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("invalid or expired");
        }
    }

    @Nested
    @DisplayName("upsertOAuthUser")
    class UpsertOAuth {
        @Test
        @DisplayName("should create new OAuth user")
        void createNew() {
            when(userRepository.findByEmailIgnoreCase("google@test.com"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(20L);
                u.setCreatedAt(LocalDateTime.now());
                u.setUpdatedAt(LocalDateTime.now());
                return u;
            });

            User result = authService.upsertOAuthUser(
                    AuthProvider.GOOGLE, "gid123", "google@test.com",
                    "Google User", "http://pic.jpg", Role.DRIVER);

            assertThat(result.getEmail()).isEqualTo("google@test.com");
            assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("should update existing user's OAuth details")
        void updateExisting() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            User result = authService.upsertOAuthUser(
                    AuthProvider.GOOGLE, "gid456", "john@test.com",
                    "John G", "http://new-pic.jpg", null);

            assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(result.getProviderId()).isEqualTo("gid456");
        }

        @Test
        @DisplayName("should throw when email is null")
        void nullEmail() {
            assertThatThrownBy(() -> authService.upsertOAuthUser(
                    AuthProvider.GOOGLE, "gid", null, "Name", null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("should use normalized email as display name when OAuth name is blank")
        void blankNameFallsBackToEmail() {
            when(userRepository.findByEmailIgnoreCase("google@test.com"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            User result = authService.upsertOAuthUser(
                    AuthProvider.GOOGLE, "gid789", " Google@Test.com ", "   ", null, null);

            assertThat(result.getEmail()).isEqualTo("google@test.com");
            assertThat(result.getFullName()).isEqualTo("google@test.com");
            assertThat(result.getRole()).isEqualTo(Role.DRIVER);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {
        @Test
        @DisplayName("should store Redis OTP and send email for local account")
        void forgotPasswordSuccess() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(redisOtpService.isRateLimited("john@test.com")).thenReturn(false);

            authService.forgotPassword(ForgotPasswordRequest.builder()
                    .email(" John@Test.com ").build());

            verify(redisOtpService).storeOtp(eq("john@test.com"), matches("\\d{6}"), anyInt());
            verify(emailService).sendOtpEmail(eq("john@test.com"), matches("\\d{6}"));
        }

        @Test
        @DisplayName("should reject password reset for social login accounts")
        void forgotPasswordSocialAccount() {
            driver.setProvider(AuthProvider.GOOGLE);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> authService.forgotPassword(ForgotPasswordRequest.builder()
                    .email("john@test.com").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("social-login");
        }

        @Test
        @DisplayName("should rate limit repeated OTP requests")
        void forgotPasswordRateLimited() {
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(redisOtpService.isRateLimited("john@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.forgotPassword(ForgotPasswordRequest.builder()
                    .email("john@test.com").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("wait 60 seconds");
        }

        @Test
        @DisplayName("should throw when password reset account is missing")
        void forgotPasswordMissingAccount() {
            when(userRepository.findByEmailIgnoreCase("missing@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword(ForgotPasswordRequest.builder()
                    .email("missing@test.com").build()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No account found");
        }
    }

    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtp {
        @Test
        @DisplayName("should verify valid OTP via Redis")
        void verifyValid() {
            when(redisOtpService.validateOtp("john@test.com", "123456")).thenReturn(true);

            assertThatCode(() -> authService.verifyOtp(
                    VerifyOtpRequest.builder().email("john@test.com").otp("123456").build()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw for invalid OTP via Redis")
        void verifyInvalid() {
            when(redisOtpService.validateOtp("john@test.com", "000000")).thenReturn(false);

            assertThatThrownBy(() -> authService.verifyOtp(
                    VerifyOtpRequest.builder().email("john@test.com").otp("000000").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {
        @Test
        @DisplayName("should reset password with valid Redis OTP")
        void resetSuccess() {
            when(redisOtpService.validateOtp("john@test.com", "123456")).thenReturn(true);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(passwordEncoder.matches("newpass123", "encodedPassword")).thenReturn(false);
            when(passwordEncoder.encode("newpass123")).thenReturn("newEncoded");

            authService.resetPassword(ResetPasswordRequest.builder()
                    .email("john@test.com").otp("123456").newPassword("newpass123").build());

            assertThat(driver.getPasswordHash()).isEqualTo("newEncoded");
            verify(redisOtpService).invalidateOtp("john@test.com");
        }

        @Test
        @DisplayName("should throw when new password same as current")
        void resetSamePassword() {
            when(redisOtpService.validateOtp("john@test.com", "123456")).thenReturn(true);
            when(userRepository.findByEmailIgnoreCase("john@test.com"))
                    .thenReturn(Optional.of(driver));
            when(passwordEncoder.matches("samepass", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.resetPassword(ResetPasswordRequest.builder()
                    .email("john@test.com").otp("123456").newPassword("samepass").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("different");
        }

        @Test
        @DisplayName("should reject invalid reset OTP")
        void resetInvalidOtp() {
            when(redisOtpService.validateOtp("john@test.com", "000000")).thenReturn(false);

            assertThatThrownBy(() -> authService.resetPassword(ResetPasswordRequest.builder()
                    .email("john@test.com").otp("000000").newPassword("newpass123").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid or expired OTP");
        }

        @Test
        @DisplayName("should throw when reset account is missing")
        void resetMissingAccount() {
            when(redisOtpService.validateOtp("missing@test.com", "123456")).thenReturn(true);
            when(userRepository.findByEmailIgnoreCase("missing@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(ResetPasswordRequest.builder()
                    .email("missing@test.com").otp("123456").newPassword("newpass123").build()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }
}
