package com.parkease.parkinglot_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger/OpenAPI metadata for the Parking Lot Service.
// Access at: http://localhost:8084/swagger-ui.html or via gateway dropdown
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Parking Lot Service API")
                        .description("CRUD operations for parking lots")
                        .version("1.0"));
    }
}
