package com.example.daugia.common.event;

import java.math.BigDecimal;

public class AuctionEndedEvent extends DomainEvent {
    private final Long auctionId;
    private final Long winnerId;
    private final BigDecimal winningAmount;
    private final String sellerEmail;
    private final String sellerName;

    public AuctionEndedEvent(Long auctionId, Long winnerId, BigDecimal winningAmount, String sellerEmail, String sellerName) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winningAmount = winningAmount;
        this.sellerEmail = sellerEmail;
        this.sellerName = sellerName;
    }

    public Long getAuctionId() { return auctionId; }
    public Long getWinnerId() { return winnerId; }
    public BigDecimal getWinningAmount() { return winningAmount; }
    public String getSellerEmail() { return sellerEmail; }
    public String getSellerName() { return sellerName; }

    @Override public String getAggregateType() { return "AUCTION"; }
    @Override public String getAggregateId() { return String.valueOf(auctionId); }
}
