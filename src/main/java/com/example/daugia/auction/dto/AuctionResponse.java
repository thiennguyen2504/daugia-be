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
    private LocalDateTime biddingEndTime;
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
