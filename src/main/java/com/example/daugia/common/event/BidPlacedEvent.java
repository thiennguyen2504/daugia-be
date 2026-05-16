package com.example.daugia.common.event;

import java.math.BigDecimal;

public class BidPlacedEvent extends DomainEvent {
    private final String auctionId;
    private final String bidId;
    private final String bidderId;
    private final BigDecimal amount;

    public BidPlacedEvent(String auctionId, String bidId, String bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidId = bidId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public String getAuctionId() { return auctionId; }
    public String getBidId() { return bidId; }
    public String getBidderId() { return bidderId; }
    public BigDecimal getAmount() { return amount; }

    @Override public String getAggregateType() { return "BID"; }
    @Override public String getAggregateId() { return bidId; }
}
