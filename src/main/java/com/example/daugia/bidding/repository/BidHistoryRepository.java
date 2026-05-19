package com.example.daugia.bidding.repository;

import com.example.daugia.bidding.entity.BidHistoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidHistoryRepository extends JpaRepository<BidHistoryEntry, String> {
    Page<BidHistoryEntry> findAllByAuctionIdOrderByBidTimeDesc(String auctionId, Pageable pageable);
    int countByAuctionId(String auctionId);
}