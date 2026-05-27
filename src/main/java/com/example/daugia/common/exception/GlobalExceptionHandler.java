package com.example.daugia.common.exception;

import com.example.daugia.backup.exception.BackupException;
import com.example.daugia.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception → HTTP response mapping.
 *
 * <p>Principles enforced here:
 * <ul>
 *   <li>WARN for client errors (4xx) — not our fault.</li>
 *   <li>ERROR + full stack trace for server bugs (5xx) — our fault.</li>
 *   <li>Security events go to the dedicated security logger.</li>
 *   <li>traceId is always included in the response so clients can report issues.</li>
 *   <li>Stack traces are NEVER included in response bodies.</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Dedicated security logger — routed to security.log in logback-spring.xml. */
    private static final Logger securityLog =
            LoggerFactory.getLogger("com.example.daugia.auth");

    // ------------------------------------------------------------------
    // Validation errors (400)
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        log.warn("Validation failed: path={} fields={}", extractPath(request), fieldErrors.keySet());

        ErrorResponse body = buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", extractPath(request), fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.error(body.getMessage(), body));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String field = v.getPropertyPath() == null ? "request" : v.getPropertyPath().toString();
            fieldErrors.put(field, v.getMessage());
        });

        log.warn("Constraint violation: path={} fields={}", extractPath(request), fieldErrors.keySet());

        ErrorResponse body = buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", extractPath(request), fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.error(body.getMessage(), body));
    }

    // ------------------------------------------------------------------
    // Application / domain errors (4xx)
    // ------------------------------------------------------------------

    @ExceptionHandler({AppException.class, ResourceNotFoundException.class,
            DuplicateResourceException.class, InvalidTokenException.class,
            EmailSendingException.class, TokenException.class, BackupException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAppException(
            AppException ex,
            WebRequest request) {

        HttpStatus status = ex.getStatus();
        if (status.is5xxServerError()) {
            log.error("Application error (5xx): path={} status={} message={}",
                    extractPath(request), status.value(), ex.getMessage(), ex);
        } else {
            log.warn("Application error (4xx): path={} status={} message={}",
                    extractPath(request), status.value(), ex.getMessage());
        }

        ErrorResponse body = buildError(status, status.name(), ex.getMessage(),
                extractPath(request), null);
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {

        log.warn("Access denied: path={}", extractPath(request));

        ErrorResponse body = buildError(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Access denied", extractPath(request), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(body.getMessage(), body));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {

        // Route to security log specifically — this is a security event
        securityLog.warn("Bad credentials: path={} traceId={}",
                extractPath(request), MDC.get("traceId"));

        ErrorResponse body = buildError(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                "Invalid username or password", extractPath(request), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(body.getMessage(), body));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex,
            WebRequest request) {

        String auctionId = MDC.get("auctionId");
        log.warn("Optimistic locking conflict: path={} auctionId={}",
                extractPath(request), auctionId);

        ErrorResponse body = buildError(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Auction was modified concurrently, please retry.", extractPath(request), null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(body.getMessage(), body));
    }

    // ------------------------------------------------------------------
    // Unhandled server errors (500)
    // ------------------------------------------------------------------

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUncategorizedException(
            RuntimeException ex,
            WebRequest request) {

        // Full stack trace here — this is a genuine server bug
        log.error("Unhandled server exception: path={} traceId={}",
                extractPath(request), MDC.get("traceId"), ex);

        ErrorResponse body = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Quote your trace ID when contacting support.",
                extractPath(request), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(body.getMessage(), body));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build a typed {@link ErrorResponse}, always injecting the current MDC traceId.
     * Stack traces are never placed here — only safe, user-facing fields.
     */
    private ErrorResponse buildError(HttpStatus status, String errorCode, String message,
                                     String path, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .traceId(MDC.get("traceId"))
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
    }

    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
