package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.entity.BidType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface BiddingService {
    BidResponse placeBid(Long auctionId, String bidderEmail, BigDecimal amount, BidType bidType);
    Page<BidResponse> getBidHistory(Long auctionId, Pageable pageable);
    BidResponse getCurrentLeader(Long auctionId);
}
