package com.example.daugia.auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction_images", indexes = {
        @Index(name = "idx_auction_image_auction", columnList = "auction_id"),
        @Index(name = "idx_auction_image_order",   columnList = "auction_id,sort_order")
})
public class AuctionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false, length = 500)
    private String publicId;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
