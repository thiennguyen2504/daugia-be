package com.example.daugia.auth.filter;

import com.example.daugia.common.utils.LogSanitizer;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String RATE_LIMIT_BODY = "{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many requests, please try again later.\"}";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        boolean limited = false;
        try {
            limited = isRateLimited(clientIp);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable for rate limiter — failing open", ex);
            limited = false;
        }

        if (limited) {
            log.warn("Rate limit exceeded for clientIp={}, path={}, timestamp={}", LogSanitizer.maskIp(clientIp), path, LocalDateTime.now());
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(RATE_LIMIT_BODY);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIp) {
        String key = "rate:auth:" + clientIp;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count != null && count > 20;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }
}
