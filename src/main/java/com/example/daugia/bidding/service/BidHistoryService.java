package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.BidHistoryEntryResponse;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.common.dto.PageResponse;

import java.math.BigDecimal;

public interface BidHistoryService {
    void record(Long auctionId, String bidderEmail, BigDecimal amount, BigDecimal increment, BidType bidType);

    PageResponse<BidHistoryEntryResponse> getHistory(Long auctionId, int page, int size);
}