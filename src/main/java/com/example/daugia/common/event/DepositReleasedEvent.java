package com.example.daugia.common.event;

public class DepositReleasedEvent extends DomainEvent {
    private final Long auctionId;
    private final Long bidderId;

    public DepositReleasedEvent(Long auctionId, Long bidderId) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
    }

    public Long getAuctionId() { return auctionId; }
    public Long getBidderId() { return bidderId; }

    @Override public String getAggregateType() { return "DEPOSIT"; }
    @Override public String getAggregateId() { return auctionId + ":" + bidderId; }
}
