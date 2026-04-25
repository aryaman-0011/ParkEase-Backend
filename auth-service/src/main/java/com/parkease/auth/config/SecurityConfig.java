package com.parkease.auth.config;

import com.parkease.auth.security.JwtAuthenticationFilter;
import com.parkease.auth.security.OAuth2AuthenticationFailureHandler;
import com.parkease.auth.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Configures Spring Security — defines which endpoints are public vs protected
@Configuration
@EnableMethodSecurity // Enables @PreAuthorize annotations on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    // ObjectProvider allows OAuth2 to be optional (app works without Google client configured)
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final AuthenticationProvider authenticationProvider;

    // Main security filter chain — defines access rules for all endpoints
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // Disable CSRF since we use JWT (stateless)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // Public endpoints — no authentication required
                                "/actuator/health",
                                "/actuator/info",
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/auth/forgot-password",
                                "/auth/verify-otp",
                                "/auth/reset-password",
                                "/auth/oauth2/google",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                // Swagger / OpenAPI docs — must be publicly accessible
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        // Admin endpoints require ADMIN role
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated())
                // Use IF_REQUIRED so OAuth2 login flow can create a session temporarily
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authenticationProvider(authenticationProvider)
                // Run our JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Only enable OAuth2 login if Google client credentials are configured
        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler));
        }

        return http.build();
    }
}
