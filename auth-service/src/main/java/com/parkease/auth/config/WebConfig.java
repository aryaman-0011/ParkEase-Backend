package com.parkease.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Maps /profile-pics/** URL path to the uploads/profile-pics/ directory on disk
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/profile-pics/**")
                .addResourceLocations("file:./uploads/profile-pics/");
    }
}
