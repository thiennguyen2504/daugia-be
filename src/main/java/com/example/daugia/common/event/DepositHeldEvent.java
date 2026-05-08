package com.example.daugia.common.event;

import java.math.BigDecimal;

public class DepositHeldEvent extends DomainEvent {
    private final Long auctionId;
    private final Long bidderId;
    private final BigDecimal amount;

    public DepositHeldEvent(Long auctionId, Long bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public Long getAuctionId() { return auctionId; }
    public Long getBidderId() { return bidderId; }
    public BigDecimal getAmount() { return amount; }

    @Override public String getAggregateType() { return "DEPOSIT"; }
    @Override public String getAggregateId() { return auctionId + ":" + bidderId; }
}
