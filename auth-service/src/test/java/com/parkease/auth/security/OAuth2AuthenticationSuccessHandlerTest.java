package com.parkease.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.service.EmailService;
import com.parkease.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock private AuthService authService;
    @Mock private EmailService emailService;
    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @Test
    void redirectsToFrontendWithTokenAndSendsLoginNotification() throws Exception {
        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService, emailService, jwtService);
        ReflectionTestUtils.setField(handler, "frontendRedirectUri", "http://localhost:4200/oauth2/success");
        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("OAuth User")
                .provider(AuthProvider.GOOGLE)
                .role(Role.DRIVER)
                .active(true)
                .build();
        when(authService.upsertOAuthUser(
                AuthProvider.GOOGLE,
                "google-id",
                "user@test.com",
                "OAuth User",
                "https://example.com/avatar.png",
                Role.DRIVER)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        var principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", "google-id",
                        "email", "user@test.com",
                        "name", "OAuth User",
                        "picture", "https://example.com/avatar.png"),
                "email");
        var authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        assertThat(redirectCaptor.getValue())
                .startsWith("http://localhost:4200/oauth2/success")
                .contains("token=jwt-token")
                .contains("email=user@test.com");
        verify(emailService).sendLoginNotificationEmail("user@test.com", "OAuth User", "Google");
    }
}
