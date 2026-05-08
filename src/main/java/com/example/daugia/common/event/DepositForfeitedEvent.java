package com.example.daugia.common.event;

public class DepositForfeitedEvent extends DomainEvent {
    private final Long auctionId;
    private final Long bidderId;

    public DepositForfeitedEvent(Long auctionId, Long bidderId) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
    }

    public Long getAuctionId() { return auctionId; }
    public Long getBidderId() { return bidderId; }

    @Override public String getAggregateType() { return "DEPOSIT"; }
    @Override public String getAggregateId() { return auctionId + ":" + bidderId; }
}
