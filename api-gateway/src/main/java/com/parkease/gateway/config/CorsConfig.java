package com.parkease.gateway.config;

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

/**
 * Custom CORS filter that ensures CORS headers are applied to ALL responses,
 * including error responses from downstream services and preflight OPTIONS.
 */
@Configuration
public class CorsConfig {

    private static final String ALLOWED_HEADERS =
            "Authorization, Content-Type, X-User-Id, X-User-Role, Accept, Origin, X-Requested-With";
    private static final String ALLOWED_METHODS =
            "GET, POST, PUT, PATCH, DELETE, OPTIONS";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String origin = request.getHeaders().getOrigin();

            if (origin == null) {
                return chain.filter(exchange);
            }

            if (!origin.matches("http://localhost:\\d+")) {
                return chain.filter(exchange);
            }

            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            // Remove any existing CORS headers to avoid duplicates
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
            headers.remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
            headers.remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);

            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
            headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ALLOWED_HEADERS);

            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }
}
