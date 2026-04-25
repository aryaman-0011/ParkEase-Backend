package com.parkease.booking_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger/OpenAPI metadata for the Booking Service.
// Access at: http://localhost:8086/swagger-ui.html or via gateway dropdown
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Booking Service API")
                        .description("Time-slot bookings, check-in/out, extend & conflict detection")
                        .version("1.0"));
    }
}
