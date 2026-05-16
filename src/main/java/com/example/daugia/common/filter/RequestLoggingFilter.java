package com.example.daugia.common.filter;

import com.example.daugia.auth.security.JwtService;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        String actor = extractActor(request);

        request.setAttribute("requestId", requestId);
        request.setAttribute("actor", actor);
        MDC.put("requestId", requestId);
        MDC.put("actor", actor);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        responseWrapper.setHeader("X-Request-Id", requestId);

        try {
            log.debug("Incoming request: method={} uri={} actor={} requestId={}", 
                    request.getMethod(), request.getRequestURI(), actor, requestId);

            filterChain.doFilter(request, responseWrapper);
            
            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("Outgoing response: method={} uri={} status={} latency_ms={} requestId={}", 
                    request.getMethod(), request.getRequestURI(), responseWrapper.getStatus(), latencyMs, requestId);
        } finally {
            MDC.clear();
            responseWrapper.copyBodyToResponse();
        }
    }

    private String extractActor(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                String subject = jwtService.extractUsername(token);
                if (subject != null && !subject.isBlank()) {
                    return subject;
                }
            } catch (Exception e) {
                // Ignore exception, token might be invalid/expired, default to ANONYMOUS
            }
        }
        return "ANONYMOUS";
    }
}