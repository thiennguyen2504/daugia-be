package com.example.daugia.common.exception;

import com.example.daugia.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> data = new HashMap<>();
        data.put("status", HttpStatus.BAD_REQUEST.value());
        data.put("path", extractPath(request));
        data.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", data));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath() == null
                    ? "request"
                    : violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        });

        Map<String, Object> data = new HashMap<>();
        data.put("status", HttpStatus.BAD_REQUEST.value());
        data.put("path", extractPath(request));
        data.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", data));
    }

    @ExceptionHandler({AppException.class, ResourceNotFoundException.class, DuplicateResourceException.class,
            InvalidTokenException.class, EmailSendingException.class, TokenException.class})
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAppException(AppException ex, WebRequest request) {
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDeniedException(Exception ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex,
            WebRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Auction was modified concurrently, please retry.", request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleUncategorizedException(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> buildErrorResponse(
            HttpStatus status,
            String message,
            WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("path", extractPath(request));
        return ResponseEntity.status(status).body(ApiResponse.error(message, body));
    }

    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
