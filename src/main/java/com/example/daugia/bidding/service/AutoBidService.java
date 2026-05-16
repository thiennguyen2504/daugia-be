package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.AutoBidConfigResponse;

import java.math.BigDecimal;

public interface AutoBidService {
    AutoBidConfigResponse upsertConfig(String auctionId, String bidderEmail, BigDecimal maxAmount);
    void deactivateConfig(String auctionId, String bidderEmail);
    AutoBidConfigResponse getOwnConfig(String auctionId, String bidderEmail);
    void processAutoBidsForAuction(String auctionId, String excludeBidderId);
}
