package com.example.daugia.auction.entity;

import com.example.daugia.category.entity.Category;
import com.example.daugia.common.audit.AuditableEntity;
import com.example.daugia.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "auctions", indexes = {
        @Index(name = "idx_auction_status",    columnList = "status"),
        @Index(name = "idx_auction_seller",    columnList = "seller_id"),
        @Index(name = "idx_auction_category",  columnList = "category_id"),
        @Index(name = "idx_auction_start",     columnList = "bidding_start_time"),
        @Index(name = "idx_auction_end",       columnList = "bidding_end_time"),
        @Index(name = "idx_auction_name",      columnList = "product_name")
})
public class Auction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal startingPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal bidIncrement;

    @Column(precision = 19, scale = 2)
    private BigDecimal buyNowPrice;

    @Column(nullable = false)
    private LocalDateTime biddingStartTime;

    @Column(nullable = false)
    private LocalDateTime biddingEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.PENDING;

    @Column(length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<AuctionImage> images = new ArrayList<>();

    @Column(length = 255)
    private String reviewedBy;

    private LocalDateTime reviewedAt;
}
