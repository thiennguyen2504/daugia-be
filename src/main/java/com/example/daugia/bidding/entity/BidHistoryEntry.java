package com.example.daugia.bidding.entity;

import com.example.daugia.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bid_history", indexes = {
        @Index(name = "idx_bh_auction_time", columnList = "auction_id,bid_time"),
        @Index(name = "idx_bh_auction_step", columnList = "auction_id,step_number")
})
public class BidHistoryEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "bidder_email_masked", nullable = false, length = 255)
    private String bidderEmailMasked;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "bid_increment_applied", nullable = false, precision = 19, scale = 2)
    private BigDecimal bidIncrementApplied;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "bid_type", nullable = false, length = 20)
    private BidType bidType;

    @Column(name = "bid_time", nullable = false, updatable = false)
    private LocalDateTime bidTime;

    @PrePersist
    void prePersist() {
        if (this.bidTime == null) {
            this.bidTime = LocalDateTime.now();
        }
    }
}