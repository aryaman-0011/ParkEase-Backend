package com.parkease.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void validation() {
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("o", "email", "required")));
        var r = handler.handleValidation(new MethodArgumentNotValidException(null, br));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(((Map<?,?>)r.getBody()).get("message").toString().contains("required"));
    }
    @Test void badRequest() {
        var r = handler.handleBadRequest(new BadRequestException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }
    @Test void unauthorized() {
        var r = handler.handleUnauthorized(new UnauthorizedException("denied"));
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }
    @Test void notFound() {
        var r = handler.handleNotFound(new ResourceNotFoundException("gone"));
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }
    @Test void accessDenied() {
        var r = handler.handleAccessDenied(new AccessDeniedException("nope"));
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
    }
    @Test void generic() {
        var r = handler.handleGeneric(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
    }
}
