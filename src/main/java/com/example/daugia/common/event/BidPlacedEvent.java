package com.example.daugia.common.event;

import java.math.BigDecimal;

public class BidPlacedEvent extends DomainEvent {
    private final Long auctionId;
    private final Long bidId;
    private final Long bidderId;
    private final BigDecimal amount;

    public BidPlacedEvent(Long auctionId, Long bidId, Long bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidId = bidId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public Long getAuctionId() { return auctionId; }
    public Long getBidId() { return bidId; }
    public Long getBidderId() { return bidderId; }
    public BigDecimal getAmount() { return amount; }

    @Override public String getAggregateType() { return "BID"; }
    @Override public String getAggregateId() { return String.valueOf(bidId); }
}
