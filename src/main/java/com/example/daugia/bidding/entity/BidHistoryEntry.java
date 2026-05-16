package com.example.daugia.bidding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bid_history_entries", indexes = {
        @Index(name = "idx_bhe_auction", columnList = "auction_id"),
        @Index(name = "idx_bhe_bid_time", columnList = "bid_time")
})
public class BidHistoryEntry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "auction_id", nullable = false, length = 36)
    private String auctionId;

    @Column(nullable = false, length = 255)
    private String bidderEmailMasked;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal bidIncrementApplied;

    @Column(nullable = false)
    private Integer stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidType bidType;

    @Column(name = "bid_time", updatable = false)
    private LocalDateTime bidTime;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.bidTime = LocalDateTime.now();
    }
}