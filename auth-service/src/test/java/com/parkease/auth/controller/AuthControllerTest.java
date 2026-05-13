package com.parkease.auth.controller;

import com.parkease.auth.dto.*;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.service.JwtService;
import com.parkease.auth.service.impl.TokenBlacklistService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthController controller;

    private AuthResponse authSample() {
        return AuthResponse.builder().accessToken("jwt").tokenType("Bearer").expiresIn(3600)
                .user(userSample()).build();
    }
    private UserResponse userSample() {
        return UserResponse.builder().id(1L).email("a@b.com").fullName("Test")
                .role(Role.DRIVER).active(true).provider(AuthProvider.LOCAL).build();
    }

    @Test void register() {
        when(authService.register(any())).thenReturn(authSample());
        var r = controller.register(new RegisterRequest());
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("jwt", r.getBody().getAccessToken());
    }
    @Test void login() {
        when(authService.login(any())).thenReturn(authSample());
        assertEquals(HttpStatus.OK, controller.login(new LoginRequest()).getStatusCode());
    }
    @Test void refresh() {
        when(authService.refreshToken("tok")).thenReturn(authSample());
        assertEquals(HttpStatus.OK, controller.refresh("Bearer tok").getStatusCode());
    }
    @Test void logout() {
        when(jwtService.getRemainingMillis("tok")).thenReturn(60000L);
        doNothing().when(tokenBlacklistService).blacklist("tok", 60000L);
        var r = controller.logout("Bearer tok");
        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(tokenBlacklistService).blacklist("tok", 60000L);
    }
    @Test void me() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@b.com");
        when(authService.getCurrentUser("a@b.com")).thenReturn(userSample());
        assertEquals(HttpStatus.OK, controller.me(auth).getStatusCode());
    }
    @Test void updateProfile() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@b.com");
        when(authService.updateProfile(eq("a@b.com"), any())).thenReturn(userSample());
        assertEquals(HttpStatus.OK, controller.updateProfile(auth, new UpdateProfileRequest()).getStatusCode());
    }
    @Test void changePassword() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@b.com");
        doNothing().when(authService).changePassword(eq("a@b.com"), any());
        assertEquals(HttpStatus.OK, controller.changePassword(auth, new ChangePasswordRequest()).getStatusCode());
    }
    @Test void deactivate() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@b.com");
        doNothing().when(authService).deactivateAccount("a@b.com");
        assertEquals(HttpStatus.OK, controller.deactivate(auth).getStatusCode());
    }
    @Test void deleteAccount() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@b.com");
        doNothing().when(authService).deleteAccount("a@b.com");
        assertEquals(HttpStatus.OK, controller.deleteAccount(auth).getStatusCode());
    }
    @Test void forgotPassword() {
        doNothing().when(authService).forgotPassword(any());
        assertEquals(HttpStatus.OK, controller.forgotPassword(new ForgotPasswordRequest()).getStatusCode());
    }
    @Test void verifyOtp() {
        doNothing().when(authService).verifyOtp(any());
        assertEquals(HttpStatus.OK, controller.verifyOtp(new VerifyOtpRequest()).getStatusCode());
    }
    @Test void resetPassword() {
        doNothing().when(authService).resetPassword(any());
        assertEquals(HttpStatus.OK, controller.resetPassword(new ResetPasswordRequest()).getStatusCode());
    }
    @Test void loginWithGoogleRedirectsToOauthAuthorization() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        controller.loginWithGoogle(response);

        verify(response).sendRedirect("/oauth2/authorization/google");
    }
}
