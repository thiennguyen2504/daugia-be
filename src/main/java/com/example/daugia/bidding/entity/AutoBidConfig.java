package com.example.daugia.bidding.entity;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auto_bid_configs",
        uniqueConstraints = @UniqueConstraint(name = "uk_auto_bid_auction_bidder", columnNames = {"auction_id", "bidder_id"}),
        indexes = {
                @Index(name = "idx_auto_bid_auction_active", columnList = "auction_id,is_active"),
                @Index(name = "idx_auto_bid_max", columnList = "auction_id,max_amount")
        })
public class AutoBidConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
