package com.parkease.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Rate limiting configuration for the API gateway.
 * Uses the client's IP address as the rate-limit key.
 * Tokens replenish at 20/sec with a burst capacity of 40 requests.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return Mono.just(forwardedFor.split(",")[0].trim());
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress == null) {
                return Mono.just("unknown");
            }

            InetAddress address = remoteAddress.getAddress();
            return Mono.just(address != null ? address.getHostAddress() : remoteAddress.getHostString());
        };
    }
}
