package com.parkease.auth.security;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.service.EmailService;
import com.parkease.auth.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${app.oauth2.redirect-uri:http://localhost:4200/oauth2/success}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        AuthProvider provider = switch (registrationId.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            default -> AuthProvider.LOCAL;
        };

        User user = authService.upsertOAuthUser(
                provider,
                userInfo.getId(),
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getImageUrl(),
                Role.DRIVER);

        String token = jwtService.generateToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("token", token)
                .queryParam("email", user.getEmail())
                .build()
                .toUriString();

        // Capitalize provider name for display (e.g. "google" -> "Google")
        String loginMethod = registrationId.substring(0, 1).toUpperCase() + registrationId.substring(1);
        emailService.sendLoginNotificationEmail(user.getEmail(), user.getFullName(), loginMethod);

        response.sendRedirect(targetUrl);
    }
}
