package com.example.daugia.bidding.repository;

import com.example.daugia.bidding.entity.BidHistoryEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidHistoryRepository extends JpaRepository<BidHistoryEntry, String> {
    Page<BidHistoryEntry> findAllByAuctionIdOrderByBidTimeDesc(String auctionId, Pageable pageable);
    int countByAuctionId(String auctionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(b) FROM BidHistoryEntry b WHERE b.auctionId = :auctionId")
    int countByAuctionIdWithLock(@Param("auctionId") String auctionId);
}