package com.parkease.vehicle_service.exception;

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

    @Test void notFound() {
        var r = handler.handleNotFound(new ResourceNotFoundException("Not found"));
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }
    @Test void badRequest() {
        var r = handler.handleBadRequest(new BadRequestException("Bad"));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }
    @Test void validation() {
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("o", "f", "required")));
        var r = handler.handleValidation(new MethodArgumentNotValidException(null, br));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }
    @Test void general() {
        var r = handler.handleGeneral(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
    }
}
