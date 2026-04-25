package com.parkease.vehicle_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger/OpenAPI metadata for the Vehicle Service.
// Access at: http://localhost:8088/swagger-ui.html or via gateway dropdown
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vehicleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Vehicle Service API")
                        .description("Vehicle registration, updates & user vehicle management")
                        .version("1.0"));
    }
}
