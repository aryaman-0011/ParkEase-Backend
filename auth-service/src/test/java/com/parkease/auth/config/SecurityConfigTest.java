package com.parkease.auth.config;

import com.parkease.auth.security.JwtAuthenticationFilter;
import com.parkease.auth.security.OAuth2AuthenticationFailureHandler;
import com.parkease.auth.security.OAuth2AuthenticationSuccessHandler;
import com.parkease.auth.service.JwtService;
import com.parkease.auth.service.impl.CustomUserDetailsService;
import com.parkease.auth.service.impl.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = SecurityConfigTest.TestSecurityContext.class)
class SecurityConfigTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("should permit public auth endpoints")
    void permitsPublicEndpoint() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    @DisplayName("should require authentication for protected endpoints")
    void requiresAuthenticationForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isFound());
    }

    @Test
    @DisplayName("should allow admin role for admin endpoints")
    void allowsAdminRole() throws Exception {
        mockMvc.perform(get("/auth/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @RestController
    static class TestController {
        @GetMapping("/auth/register")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/profile")
        String protectedEndpoint() {
            return "profile";
        }

        @GetMapping("/auth/admin/users")
        String adminEndpoint() {
            return "admin";
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, TestController.class})
    static class TestSecurityContext {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService,
                CustomUserDetailsService userDetailsService,
                TokenBlacklistService tokenBlacklistService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, tokenBlacklistService);
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        CustomUserDetailsService userDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }

        @Bean
        OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
            return mock(OAuth2AuthenticationSuccessHandler.class);
        }

        @Bean
        OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
            return mock(OAuth2AuthenticationFailureHandler.class);
        }

        @Bean
        AuthenticationProvider authenticationProvider() {
            return mock(AuthenticationProvider.class);
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }
    }
}
