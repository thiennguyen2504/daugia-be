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
@Table(name = "bids", indexes = {
        @Index(name = "idx_bid_auction_time", columnList = "auction_id,bid_time"),
        @Index(name = "idx_bid_auction_amount", columnList = "auction_id,amount")
})
public class Bid {

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
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidType bidType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status = BidStatus.ACCEPTED;
}
