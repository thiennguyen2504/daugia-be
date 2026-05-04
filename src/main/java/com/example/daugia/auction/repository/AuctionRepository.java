package com.example.daugia.auction.repository;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Page<Auction> findAllByStatusIn(List<AuctionStatus> statuses, Pageable pageable);

    Page<Auction> findAllBySeller_Id(Long sellerId, Pageable pageable);

    @Query("SELECT a FROM Auction a WHERE a.status = 'APPROVED' AND a.biddingStartTime <= :now")
    List<Auction> findApprovedReadyToActivate(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.biddingEndTime <= :now")
    List<Auction> findActiveReadyToEnd(@Param("now") LocalDateTime now);

    @Query("""
            SELECT a FROM Auction a
            WHERE a.status IN ('APPROVED', 'ACTIVE', 'ENDED')
            AND (:search IS NULL OR LOWER(a.productName) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:categoryId IS NULL OR a.category.id = :categoryId)
            AND (:minPrice IS NULL OR a.startingPrice >= :minPrice)
            AND (:maxPrice IS NULL OR a.startingPrice <= :maxPrice)
            AND (:startFrom IS NULL OR a.biddingStartTime >= :startFrom)
            AND (:startTo IS NULL OR a.biddingStartTime <= :startTo)
            """)
    Page<Auction> searchPublic(
            @Param("search")     String search,
            @Param("categoryId") Long categoryId,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("startFrom")  LocalDateTime startFrom,
            @Param("startTo")    LocalDateTime startTo,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM Auction a
            WHERE (:status IS NULL OR a.status = :status)
            AND (:search IS NULL OR LOWER(a.productName) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:categoryId IS NULL OR a.category.id = :categoryId)
            """)
    Page<Auction> searchAdmin(
            @Param("status")     AuctionStatus status,
            @Param("search")     String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}
