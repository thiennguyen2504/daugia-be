package com.example.daugia.auction.repository;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, String>, JpaSpecificationExecutor<Auction> {

    Page<Auction> findAllByStatusIn(List<AuctionStatus> statuses, Pageable pageable);

    Page<Auction> findAllBySeller_Id(String sellerId, Pageable pageable);

    @Query("SELECT a FROM Auction a WHERE a.status = 'APPROVED' AND a.biddingStartTime <= :now")
    List<Auction> findApprovedReadyToActivate(@Param("now") LocalDateTime now);

    // endTime is the effective end time (may be extended by anti-snipe);
    // falls back to biddingEndTime for auctions approved before anti-snipe was introduced.
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND COALESCE(a.endTime, a.biddingEndTime) <= :now")
    List<Auction> findActiveReadyToEnd(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") String id);

    @Query("SELECT DISTINCT a FROM Auction a " +
           "LEFT JOIN FETCH a.images " +
           "LEFT JOIN FETCH a.seller " +
           "LEFT JOIN FETCH a.category " +
           "WHERE a.id IN :ids")
    List<Auction> findAllWithImagesByIds(@Param("ids") List<String> ids);
}
