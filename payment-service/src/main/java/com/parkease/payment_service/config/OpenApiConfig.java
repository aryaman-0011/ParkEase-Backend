package com.parkease.payment_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger/OpenAPI metadata for the Payment Service.
// Access at: http://localhost:8087/swagger-ui.html or via gateway dropdown
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Payment Service API")
                        .description("Payment processing, Razorpay integration & payment history")
                        .version("1.0"));
    }
}
