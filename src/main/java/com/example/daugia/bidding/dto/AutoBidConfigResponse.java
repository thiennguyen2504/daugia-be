package com.example.daugia.bidding.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AutoBidConfigResponse {
    private Long auctionId;
    private BigDecimal maxAmount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
