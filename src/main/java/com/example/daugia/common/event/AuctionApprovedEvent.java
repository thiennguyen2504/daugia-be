package com.example.daugia.common.event;

import java.time.LocalDateTime;

public class AuctionApprovedEvent extends DomainEvent {

    private final Long auctionId;
    private final String productName;
    private final String sellerEmail;
    private final String sellerName;
    private final LocalDateTime biddingStartTime;

    public AuctionApprovedEvent(Long auctionId, String productName,
                                String sellerEmail, String sellerName,
                                LocalDateTime biddingStartTime) {
        this.auctionId        = auctionId;
        this.productName      = productName;
        this.sellerEmail      = sellerEmail;
        this.sellerName       = sellerName;
        this.biddingStartTime = biddingStartTime;
    }

    public Long getAuctionId()              { return auctionId; }
    public String getProductName()          { return productName; }
    public String getSellerEmail()          { return sellerEmail; }
    public String getSellerName()           { return sellerName; }
    public LocalDateTime getBiddingStartTime() { return biddingStartTime; }

    @Override public String getAggregateType() { return "AUCTION"; }
    @Override public String getAggregateId()   { return String.valueOf(auctionId); }
}
