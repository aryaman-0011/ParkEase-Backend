package com.parkease.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterConfigTest {

    @Test
    void ipKeyResolverReturnsIp() {
        var config = new RateLimiterConfig();
        KeyResolver resolver = config.ipKeyResolver();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
        String key = resolver.resolve(exchange).block();
        assertNotNull(key);
    }
}
