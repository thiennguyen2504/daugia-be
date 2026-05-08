package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.BidResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisBidPublisher {
    public static final String CHANNEL = "auction-bid-events";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(BidResponse response) {
        try {
            stringRedisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize bid response for Redis pub/sub", ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while publishing bid event", ex);
        }
    }
}
