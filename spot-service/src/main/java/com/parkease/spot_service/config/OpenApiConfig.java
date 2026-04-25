package com.parkease.spot_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger/OpenAPI metadata for the Spot Service.
// Access at: http://localhost:8085/swagger-ui.html or via gateway dropdown
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI spotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Spot Service API")
                        .description("Parking spot management, status updates & availability counts")
                        .version("1.0"));
    }
}
