package com.parkease.booking_service.exception;

import com.parkease.booking_service.dto.ApiMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void handleNotFound() {
        var resp = handler.handleNotFound(new ResourceNotFoundException("Booking not found"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Booking not found", resp.getBody().getMessage());
    }

    @Test void handleBadRequest() {
        var resp = handler.handleBadRequest(new BadRequestException("Invalid time"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test void handleValidation() {
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "name", "must not be null")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, br);
        var resp = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().getMessage().contains("name"));
    }

    @Test void handleGeneral() {
        var resp = handler.handleGeneral(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertTrue(resp.getBody().getMessage().contains("boom"));
    }
}
