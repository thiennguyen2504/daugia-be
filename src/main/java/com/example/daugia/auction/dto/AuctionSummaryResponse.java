package com.example.daugia.auction.dto;

import com.example.daugia.auction.entity.AuctionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionSummaryResponse {
    private String id;
    private String productName;
    private BigDecimal startingPrice;
    private BigDecimal buyNowPrice;
    private AuctionStatus status;
    private LocalDateTime biddingStartTime;
    private LocalDateTime biddingEndTime;
    /**
     * Effective end time; may be extended by anti-snipe logic.
     */
    private LocalDateTime endTime;
    private String categoryName;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
}
