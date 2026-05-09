package com.parkease.parkinglot_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void notFound() {
        var r = handler.handleNotFound(new ResourceNotFoundException("Lot not found"));
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertTrue(((Map<?,?>)r.getBody()).containsKey("message"));
    }
    @Test void badRequest() {
        var r = handler.handleBadRequest(new BadRequestException("Invalid"));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }
    @Test void unauthorized() {
        var r = handler.handleUnauthorized(new UnauthorizedException("Denied"));
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }
    @Test void validation() {
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("o", "name", "required")));
        var r = handler.handleValidation(new MethodArgumentNotValidException(null, br));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }
    @Test void missingHeader() {
        MethodParameter mp = mock(MethodParameter.class);
        when(mp.getNestedParameterType()).thenReturn((Class) Long.class);
        var ex = new MissingRequestHeaderException("X-User-Id", mp);
        var r = handler.handleMissingHeader(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }
    @Test void general() {
        var r = handler.handleGeneric(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
    }
}
