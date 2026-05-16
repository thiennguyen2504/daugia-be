package com.example.daugia.bidding.repository;

import com.example.daugia.bidding.entity.Bid;
import com.example.daugia.bidding.entity.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, String> {
    Optional<Bid> findTopByAuctionIdAndStatusOrderByAmountDesc(String auctionId, BidStatus status);

    Page<Bid> findAllByAuctionIdOrderByBidTimeDesc(String auctionId, Pageable pageable);
}
