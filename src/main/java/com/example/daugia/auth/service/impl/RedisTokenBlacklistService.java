package com.example.daugia.auth.service.impl;

import com.example.daugia.auth.service.TokenBlacklistService;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklistToken(String token) {
        long remainingTtl = remainingTtlSeconds(token);
        if (remainingTtl <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(PREFIX + token, "1", Duration.ofSeconds(remainingTtl));
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while blacklisting token", ex);
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while checking token blacklist", ex);
            return false;
        }
    }

    private long remainingTtlSeconds(String token) {
        try {
            Date expiration = SignedJWT.parse(token).getJWTClaimsSet().getExpirationTime();
            if (expiration == null) {
                return 0;
            }
            return Duration.between(Instant.now(), expiration.toInstant()).getSeconds();
        } catch (ParseException ex) {
            return 0;
        }
    }
}
