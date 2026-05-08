package com.example.daugia.bidding.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LeaderboardEntryResponse {
    private String bidderEmail;
    private BigDecimal amount;
}
