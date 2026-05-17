package com.example.daugia.bidding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private String auctionId;
    private String bidId;
    private BigDecimal amount;
    private BigDecimal currentPrice;
    private String winnerEmail;
    private LocalDateTime endTime;
    private String status;
    private String rejectionReason;
    private LocalDateTime bidTime;
    private LocalDateTime updatedAt;
}
