package com.example.daugia.bidding.dto;

import com.example.daugia.bidding.entity.BidType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BidHistoryEntryResponse {
    private Long id;
    private Long auctionId;
    private String bidderEmailMasked;
    private BigDecimal amount;
    private BigDecimal bidIncrementApplied;
    private Integer stepNumber;
    private BidType bidType;
    private LocalDateTime bidTime;
}