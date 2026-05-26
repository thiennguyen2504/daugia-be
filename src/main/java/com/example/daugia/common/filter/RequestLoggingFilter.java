package com.example.daugia.common.filter;

import com.example.daugia.auth.security.JwtService;
import com.example.daugia.common.logging.LogContext;
import com.example.daugia.common.logging.LogField;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Per-request MDC population and structured request/response logging.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Resolve or generate a traceId (checks {@code X-B3-TraceId} header first).</li>
 *   <li>Extract actor from JWT (falls back to {@code ANONYMOUS}).</li>
 *   <li>Populate MDC via {@link LogContext} so every downstream log line carries context.</li>
 *   <li>Set {@code X-Trace-Id} response header for client-side error reporting.</li>
 *   <li>Log incoming request at DEBUG (too noisy for INFO); response at INFO.</li>
 *   <li>Never log request bodies on sensitive auth paths.</li>
 *   <li>Never log latency — that belongs in Micrometer metrics, not logs.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** Paths whose bodies must never be logged (contain credentials). */
    private static final Set<String> SENSITIVE_PATH_PREFIXES = Set.of(
            "/api/v1/auth/"
    );

    private static final String HEADER_B3_TRACE_ID = "X-B3-TraceId";
    private static final String HEADER_TRACE_ID_OUT = "X-Trace-Id";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request);
        String actor   = extractActor(request);
        String method  = request.getMethod();
        String path    = request.getRequestURI();

        // Expose traceId to downstream code via request attribute
        request.setAttribute("traceId", traceId);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        responseWrapper.setHeader(HEADER_TRACE_ID_OUT, traceId);

        try (var ctx = LogContext.of(LogField.TRACE_ID, traceId)
                                 .and(LogField.ACTOR, actor)
                                 .and(LogField.HTTP_METHOD, method)
                                 .and(LogField.HTTP_PATH, path)
                                 .build()) {

            log.debug("Incoming request: method={} path={} actor={} traceId={}",
                    method, path, actor, traceId);

            filterChain.doFilter(request, responseWrapper);

            int status = responseWrapper.getStatus();
            MDC.put(LogField.HTTP_STATUS.key(), String.valueOf(status));

            log.info("Request completed: method={} path={} status={} traceId={}",
                    method, path, status, traceId);

        } catch (Exception ex) {
            log.error("Unhandled exception during request: method={} path={} traceId={}",
                    method, path, traceId, ex);
            throw ex;
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Use the incoming B3 traceId if present (for distributed tracing propagation);
     * otherwise generate a new UUID-based traceId.
     */
    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_B3_TRACE_ID);
        if (incoming != null && !incoming.isBlank()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Extract the authenticated user's email from the JWT Bearer token.
     * Returns {@code "ANONYMOUS"} if no valid token is present.
     */
    private String extractActor(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token   = authorization.substring(7);
                String subject = jwtService.extractUsername(token);
                if (subject != null && !subject.isBlank()) {
                    return subject;
                }
            } catch (Exception ignored) {
                // Invalid / expired token — caller will be treated as ANONYMOUS
            }
        }
        return "ANONYMOUS";
    }

    /** Returns {@code true} for paths that must never have their bodies logged. */
    private boolean isSensitivePath(String path) {
        return SENSITIVE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}