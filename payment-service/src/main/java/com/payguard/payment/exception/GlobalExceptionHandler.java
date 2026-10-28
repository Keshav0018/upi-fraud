package com.payguard.payment.exception;

import com.payguard.payment.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Validation failed", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), "Not Found",
                ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidStateException ex, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), "Conflict",
                ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request",
                ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.badRequest().body(body);
    }
}
