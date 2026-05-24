package com.example.daugia.payment.service.impl;

import com.example.daugia.payment.service.BuyNowReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyNowReservationServiceImpl implements BuyNowReservationService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String RESERVATION_KEY_PREFIX = "buynow:reservation:";

    @Override
    public Optional<String> getReservationHolder(String auctionId) {
        try {
            return Optional.ofNullable(stringRedisTemplate.opsForValue().get(RESERVATION_KEY_PREFIX + auctionId));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed while getting reservation for auction: {}", auctionId, e);
            return Optional.empty();
        }
    }

    @Override
    public void createReservation(String auctionId, String bidderEmail, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(RESERVATION_KEY_PREFIX + auctionId, bidderEmail, ttl);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed while creating reservation for auction: {}", auctionId, e);
        }
    }

    @Override
    public void clearReservation(String auctionId) {
        try {
            stringRedisTemplate.delete(RESERVATION_KEY_PREFIX + auctionId);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed while clearing reservation for auction: {}", auctionId, e);
        }
    }

    @Override
    public Optional<Long> getRemainingSeconds(String auctionId) {
        try {
            Long expire = stringRedisTemplate.getExpire(RESERVATION_KEY_PREFIX + auctionId);
            if (expire != null && expire > 0) {
                return Optional.of(expire);
            }
            return Optional.empty();
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed while getting remaining seconds for auction: {}", auctionId, e);
            return Optional.empty();
        }
    }
}
