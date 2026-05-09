package com.parkease.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Maps /profile-pics/** and /avatars/** URL paths to upload directories on disk
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/profile-pics/**")
                .addResourceLocations("file:./uploads/profile-pics/");
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:./uploads/avatars/");
    }
}
