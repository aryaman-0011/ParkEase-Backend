package com.parkease.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger/OpenAPI metadata for this service.
// Access Swagger UI at: http://localhost:8081/swagger-ui.html
// Or via gateway at: http://localhost:8080/swagger-ui.html (select "Auth Service" dropdown)
@Configuration
public class OpenApiConfig {

    // Define API title, description, and version shown in Swagger UI
    @Bean
    public OpenAPI authOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Auth Service API")
                        .description("Authentication, registration, user management & JWT tokens")
                        .version("1.0"));
    }
}
