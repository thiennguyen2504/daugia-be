package com.example.daugia.auction.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionImageResponse {
    private String id;
    private String imageUrl;
    private Integer sortOrder;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}

