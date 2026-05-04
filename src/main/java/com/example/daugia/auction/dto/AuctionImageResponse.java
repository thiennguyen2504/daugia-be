package com.example.daugia.auction.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionImageResponse {
    private Long id;
    private String imageUrl;
    private Integer sortOrder;
    private LocalDateTime uploadedAt;
}
