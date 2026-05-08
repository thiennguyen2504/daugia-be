package com.example.daugia.bidding.service.impl;

import com.example.daugia.bidding.dto.LeaderboardEntryResponse;
import com.example.daugia.bidding.service.LeaderboardService;
import com.example.daugia.bidding.util.EmailMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void updateLeaderboard(Long auctionId, String bidderEmail, BigDecimal amount, LocalDateTime endTime) {
        try {
            String key = key(auctionId);
            stringRedisTemplate.opsForZSet().add(key, bidderEmail, amount.doubleValue());
            LocalDateTime expiryAt = endTime == null ? LocalDateTime.now().plusHours(24) : endTime.plusHours(24);
            long seconds = Math.max(60, Duration.between(LocalDateTime.now(), expiryAt).getSeconds());
            stringRedisTemplate.expire(key, Duration.ofSeconds(seconds));
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while updating leaderboard", ex);
        }
    }

    @Override
    public List<LeaderboardEntryResponse> getTop(Long auctionId) {
        try {
            var tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key(auctionId), 0, 9);
            if (tuples == null) {
                return List.of();
            }
            return tuples.stream()
                    .map(this::toResponse)
                    .toList();
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while reading leaderboard", ex);
            return Collections.emptyList();
        }
    }

    private LeaderboardEntryResponse toResponse(ZSetOperations.TypedTuple<String> tuple) {
        return LeaderboardEntryResponse.builder()
                .bidderEmail(EmailMaskingUtils.mask(tuple.getValue()))
                .amount(BigDecimal.valueOf(tuple.getScore() == null ? 0 : tuple.getScore()))
                .build();
    }

    private String key(Long auctionId) {
        return "auction:leaderboard:" + auctionId;
    }
}
