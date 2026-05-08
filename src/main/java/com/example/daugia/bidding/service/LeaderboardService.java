package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.LeaderboardEntryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LeaderboardService {
    void updateLeaderboard(Long auctionId, String bidderEmail, BigDecimal amount, LocalDateTime endTime);
    List<LeaderboardEntryResponse> getTop(Long auctionId);
}
