package com.parkease.gateway.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.WebFilter;

// Custom CORS filter that ensures CORS headers are applied to ALL responses,
// including error responses from downstream services and preflight OPTIONS requests.
// This is needed because Spring Cloud Gateway's built-in CORS doesn't always cover error responses.
@Configuration
public class CorsConfig {

    // Headers the browser is allowed to send
    private static final String ALLOWED_HEADERS =
            "Authorization, Content-Type, X-User-Id, X-User-Role, Accept, Origin, X-Requested-With";
    // HTTP methods allowed from the browser
    private static final String ALLOWED_METHODS =
            "GET, POST, PUT, PATCH, DELETE, OPTIONS";

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins = "http://localhost:4200";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // Run before ALL other filters
    public WebFilter corsFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String origin = request.getHeaders().getOrigin();

            // No Origin header = not a CORS request (e.g. server-to-server call)
            if (origin == null) {
                return chain.filter(exchange);
            }

            if (!isAllowedOrigin(origin)) {
                return chain.filter(exchange);
            }

            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            // Remove any existing CORS headers to avoid duplicates from downstream services
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
            headers.remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);

            // Set CORS headers for the response
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
            headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"); // Cache preflight for 1 hour
            headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ALLOWED_HEADERS);

            // For OPTIONS preflight requests, return 200 immediately without forwarding
            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin.matches("http://localhost:\\d+") || origin.matches("http://127\\.0\\.0\\.1:\\d+")) {
            return true;
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(allowedOrigin -> !allowedOrigin.isEmpty())
                .anyMatch(origin::equals);
    }
}
