package com.parkease.gateway.filter;

import com.parkease.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Gateway-level global filter that validates JWT tokens before routing.
 *
 * <p>For protected endpoints, this filter:
 * <ol>
 *   <li>Extracts the Bearer token from the Authorization header</li>
 *   <li>Validates the token signature and expiration using the shared secret</li>
 *   <li>Extracts userId and role from the token claims</li>
 *   <li>Forwards X-User-Id and X-User-Role headers to downstream services</li>
 * </ol>
 *
 * <p>Public endpoints (login, register, Swagger, etc.) bypass this filter entirely.
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    // Paths that should never require authentication
    private static final List<String> OPEN_PATHS = List.of(
            "/v3/api-docs",         // OpenAPI spec endpoint
            "/swagger-ui",          // Swagger UI static assets
            "/swagger-ui.html",     // Swagger UI entry point
            "/webjars/",            // Swagger UI webjars
            "/services/",           // Aggregated API-docs routes proxied from each service
            "/actuator",            // Health check and monitoring
            "/auth/register",       // Public auth endpoints
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/auth/forgot-password",
            "/auth/verify-otp",
            "/auth/reset-password",
            "/auth/internal/",      // Internal service-to-service calls
            "/auth/oauth2",
            "/oauth2",
            "/login/oauth2",
            "/avatars/",            // Public avatar images
            "/profile-pics/",       // Public profile pictures
            "/lots/search",         // Public lot search
            "/lots/nearby",         // Public nearby search
            "/lots"                 // Public GET all approved lots
    );

    // Paths that are public only for GET requests
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/lots",
            "/spots"
    );

    public AuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Skip auth for public endpoints
        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        // Allow public GET on specific paths (e.g., GET /lots, GET /lots/{id}, GET /spots/{lotId})
        if ("GET".equals(method) && isPublicGetPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Bearer token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // Validate token
        Claims claims = jwtUtil.validateAndGetClaims(token);
        if (claims == null) {
            log.warn("Invalid or expired JWT token for {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract user info from token claims
        String userId = String.valueOf(claims.get("userId", Integer.class));
        String role = claims.get("role", String.class);
        String email = claims.getSubject();

        // Forward user context to downstream services via headers
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .header("X-User-Email", email)
                .build();

        log.debug("JWT validated: user={} role={} path={}", email, role, path);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // Check if the request path matches any open path prefix
    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    // Check if the request is a public GET path
    private boolean isPublicGetPath(String path) {
        return PUBLIC_GET_PATHS.stream().anyMatch(p ->
                path.equals(p) || path.startsWith(p + "/"));
    }

    @Override
    public int getOrder() {
        return -1; // Run before other gateway filters
    }
}
