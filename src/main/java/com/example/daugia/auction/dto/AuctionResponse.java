package com.example.daugia.auction.dto;

import com.example.daugia.auction.entity.AuctionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionResponse {
    private String id;
    private String productName;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal bidIncrement;
    private BigDecimal buyNowPrice;
    private AuctionStatus status;
    private String rejectionReason;
    private LocalDateTime biddingStartTime;
    /**
     * biddingEndTime — original end time as submitted by seller, never changes.
     */
    private LocalDateTime biddingEndTime;

    /**
     * endTime — effective end time, may be extended by anti-snipe logic.
     */
    private LocalDateTime endTime;
    /** Number of times the auction end time was extended by anti-snipe logic. */
    private int extensionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Seller info (flattened)
    private String sellerId;
    private String sellerEmail;
    private String sellerName;
    // Category info
    private String categoryId;
    private String categoryName;
    // Images
    private List<AuctionImageResponse> images;
    private String thumbnailUrl;
}
