package com.example.daugia.deposit.entity;

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
@Table(name = "deposits",
        uniqueConstraints = @UniqueConstraint(name = "uk_deposit_auction_bidder", columnNames = {"auction_id", "bidder_id"}),
        indexes = {
                @Index(name = "idx_deposit_auction", columnList = "auction_id"),
                @Index(name = "idx_deposit_bidder", columnList = "bidder_id")
        })
public class Deposit {

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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepositStatus status = DepositStatus.HELD;

    private LocalDateTime heldAt;

    private LocalDateTime releasedAt;
}
