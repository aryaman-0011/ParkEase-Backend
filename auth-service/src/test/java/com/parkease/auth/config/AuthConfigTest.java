package com.parkease.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkease.auth.service.impl.CustomUserDetailsService;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class AuthConfigTest {

    @Test
    void openApiConfigBuildsAuthApiMetadata() {
        OpenAPI openAPI = new OpenApiConfig().authOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("ParkEase Auth Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
    }

    @Test
    void redisConfigCreatesStringRedisTemplate() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        StringRedisTemplate template = new RedisConfig().stringRedisTemplate(connectionFactory);

        assertThat(template).isNotNull();
    }

    @Test
    void webConfigRegistersUploadResourceHandlers() {
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration profilePics = mock(ResourceHandlerRegistration.class);
        ResourceHandlerRegistration avatars = mock(ResourceHandlerRegistration.class);
        when(registry.addResourceHandler("/profile-pics/**")).thenReturn(profilePics);
        when(registry.addResourceHandler("/avatars/**")).thenReturn(avatars);
        when(profilePics.addResourceLocations("file:./uploads/profile-pics/")).thenReturn(profilePics);
        when(avatars.addResourceLocations("file:./uploads/avatars/")).thenReturn(avatars);

        new WebConfig().addResourceHandlers(registry);

        verify(profilePics).addResourceLocations("file:./uploads/profile-pics/");
        verify(avatars).addResourceLocations("file:./uploads/avatars/");
    }

    @Test
    void securityBeansConfigCreatesPasswordEncoderAndAuthenticationProvider() {
        SecurityBeansConfig config = new SecurityBeansConfig(mock(CustomUserDetailsService.class));

        PasswordEncoder passwordEncoder = config.passwordEncoder();
        AuthenticationProvider provider = config.authenticationProvider(passwordEncoder);

        assertThat(passwordEncoder.encode("secret")).isNotBlank();
        assertThat(provider).isNotNull();
    }

    @Test
    void securityBeansConfigReturnsAuthenticationManagerFromSpringConfiguration() throws Exception {
        SecurityBeansConfig config = new SecurityBeansConfig(mock(CustomUserDetailsService.class));
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertThat(config.authenticationManager(authenticationConfiguration)).isSameAs(authenticationManager);
    }

    @Test
    void securityBeansConfigWrapsAuthenticationManagerInitializationFailure() throws Exception {
        SecurityBeansConfig config = new SecurityBeansConfig(mock(CustomUserDetailsService.class));
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> config.authenticationManager(authenticationConfiguration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to initialize");
    }
}
