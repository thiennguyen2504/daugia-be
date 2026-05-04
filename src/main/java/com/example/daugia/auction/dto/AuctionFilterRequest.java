package com.example.daugia.auction.dto;

import com.example.daugia.auction.entity.AuctionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionFilterRequest {
    private String search;
    private Long categoryId;
    private AuctionStatus status;
    private LocalDateTime startFrom;
    private LocalDateTime startTo;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    private String sortDir;
}
