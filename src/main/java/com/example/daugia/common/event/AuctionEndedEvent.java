package com.example.daugia.common.event;

import java.math.BigDecimal;

public class AuctionEndedEvent extends DomainEvent {
    private final String auctionId;
    private final String winnerId;
    private final BigDecimal winningAmount;
    private final String productName;
    private final String sellerEmail;
    private final String sellerName;
    private final String winnerEmail;
    private final String winnerName;

    public AuctionEndedEvent(String auctionId,
                            String winnerId,
                            BigDecimal winningAmount,
                            String productName,
                            String sellerEmail,
                            String sellerName,
                            String winnerEmail,
                            String winnerName) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winningAmount = winningAmount;
        this.productName = productName;
        this.sellerEmail = sellerEmail;
        this.sellerName = sellerName;
        this.winnerEmail = winnerEmail;
        this.winnerName = winnerName;
    }

    public String getAuctionId() { return auctionId; }
    public String getWinnerId() { return winnerId; }
    public BigDecimal getWinningAmount() { return winningAmount; }
    public String getProductName() { return productName; }
    public String getSellerEmail() { return sellerEmail; }
    public String getSellerName() { return sellerName; }
    public String getWinnerEmail() { return winnerEmail; }
    public String getWinnerName() { return winnerName; }

    @Override public String getAggregateType() { return "AUCTION"; }
    @Override public String getAggregateId() { return auctionId; }
}
