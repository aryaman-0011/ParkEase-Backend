package com.parkease.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

// Gateway-level global filter that runs before routing to any downstream service.
// Currently acts as a pass-through but explicitly skips Swagger/OpenAPI and public
// paths so they remain accessible. JWT validation can be added here in the future.
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    // Paths that should never be intercepted by auth logic
    private static final List<String> OPEN_PATHS = List.of(
            "/v3/api-docs",         // OpenAPI spec endpoint
            "/swagger-ui",          // Swagger UI static assets
            "/swagger-ui.html",     // Swagger UI entry point
            "/services/",           // Aggregated API-docs routes proxied from each service
            "/actuator",            // Health check and monitoring
            "/auth/register",       // Public auth endpoints
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/auth/forgot-password",
            "/auth/verify-otp",
            "/auth/reset-password",
            "/auth/oauth2",
            "/oauth2",
            "/login/oauth2"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip auth checks for open/public endpoints
        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        // ── Future: Add JWT validation here ──
        // String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        //     exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        //     return exchange.getResponse().setComplete();
        // }
        // ... validate token, extract claims, add X-User-Id header ...

        return chain.filter(exchange);
    }

    // Check if the request path starts with any open path prefix
    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1; // Run before other gateway filters
    }
}
