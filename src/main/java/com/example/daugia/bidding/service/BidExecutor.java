package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.entity.BidType;

import java.math.BigDecimal;

public interface BidExecutor {
    BidResponse execute(Long auctionId, String bidderEmail, BigDecimal amount, BidType bidType);
}
