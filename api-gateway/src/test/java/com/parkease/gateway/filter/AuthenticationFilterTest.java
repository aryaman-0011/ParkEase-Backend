package com.parkease.gateway.filter;

import com.parkease.gateway.util.JwtUtil;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.*;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest {
    private JwtUtil jwtUtil;
    private AuthenticationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach void setUp() {
        jwtUtil = mock(JwtUtil.class);
        filter = new AuthenticationFilter(jwtUtil);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test void publicPath() {
        var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login").build());
        filter.filter(ex, chain).block();
        verify(chain).filter(ex);
    }

    @Test void swagger() {
        var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/v3/api-docs/a").build());
        filter.filter(ex, chain).block();
        verify(chain).filter(ex);
    }

    @Test void noToken401() {
        var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/bookings/1").build());
        filter.filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test void invalidToken401() {
        var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/bookings/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad").build());
        when(jwtUtil.validateAndGetClaims("bad")).thenReturn(null);
        filter.filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test void validTokenForwards() {
        var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/bookings/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ok").build());
        var c = new DefaultClaims(Map.of("userId", 42, "role", "DRIVER", "sub", "a@b.com",
                "exp", new Date(System.currentTimeMillis() + 60000)));
        when(jwtUtil.validateAndGetClaims("ok")).thenReturn(c);
        filter.filter(ex, chain).block();
        verify(chain).filter(any());
    }

    @Test void publicGetLots() {
        var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/lots").build());
        filter.filter(ex, chain).block();
        verify(chain).filter(ex);
    }

    @Test void order() { assertEquals(-1, filter.getOrder()); }
}
