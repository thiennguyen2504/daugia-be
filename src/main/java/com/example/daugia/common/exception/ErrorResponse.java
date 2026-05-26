package com.example.daugia.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * RFC 7807-inspired Problem Details error response body.
 *
 * <p>The {@code traceId} field lets clients include it in bug reports so engineers
 * can correlate it with server-side logs — without ever exposing stack traces.
 *
 * <p>Used exclusively by {@link GlobalExceptionHandler}; never returned as raw Map.
 */
@Data
@Builder
public class ErrorResponse {

    /** Trace/correlation ID from MDC — clients use this to report issues to support. */
    private String traceId;

    /** HTTP status code (redundant with HTTP header, but helpful for consumers). */
    private int status;

    /** Short machine-readable error code, e.g. {@code "VALIDATION_FAILED"}. */
    private String error;

    /** Human-friendly message safe to display in UI. No internal details. */
    private String message;

    /** Request path, for context. */
    private String path;

    /** Wall-clock time of the error. */
    private Instant timestamp;

    /**
     * Per-field validation errors.
     * Only populated for {@code 400 VALIDATION_FAILED} responses; {@code null} otherwise.
     */
    private Map<String, String> fieldErrors;
}
