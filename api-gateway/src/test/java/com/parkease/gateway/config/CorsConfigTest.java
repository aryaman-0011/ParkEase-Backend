package com.parkease.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();
    private final WebFilter filter = corsConfig.corsFilter();

    @Test
    void noOriginSkipsCors() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        filter.filter(exchange, chain).block();
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        verify(chain).filter(exchange);
    }

    @Test
    void localhostOriginSetsCorsHeaders() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("Origin", "http://localhost:4200").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        filter.filter(exchange, chain).block();
        assertEquals("http://localhost:4200",
                exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true",
                exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void nonLocalhostOriginSkipsCors() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("Origin", "https://evil.com").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        filter.filter(exchange, chain).block();
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void optionsPreflightReturns200() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/test").header("Origin", "http://localhost:4200").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        filter.filter(exchange, chain).block();
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }
}
