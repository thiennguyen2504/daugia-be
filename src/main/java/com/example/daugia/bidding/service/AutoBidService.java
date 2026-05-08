package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.AutoBidConfigResponse;

import java.math.BigDecimal;

public interface AutoBidService {
    AutoBidConfigResponse upsertConfig(Long auctionId, String bidderEmail, BigDecimal maxAmount);
    void deactivateConfig(Long auctionId, String bidderEmail);
    AutoBidConfigResponse getOwnConfig(Long auctionId, String bidderEmail);
    void processAutoBidsForAuction(Long auctionId, Long excludeBidderId);
}
