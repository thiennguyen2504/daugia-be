package com.example.daugia.common.event;

public class AuctionCreatedEvent extends DomainEvent {

    private final Long auctionId;
    private final String productName;
    private final String sellerEmail;

    public AuctionCreatedEvent(Long auctionId, String productName, String sellerEmail) {
        this.auctionId   = auctionId;
        this.productName = productName;
        this.sellerEmail = sellerEmail;
    }

    public Long getAuctionId()    { return auctionId; }
    public String getProductName() { return productName; }
    public String getSellerEmail() { return sellerEmail; }

    @Override public String getAggregateType() { return "AUCTION"; }
    @Override public String getAggregateId()   { return String.valueOf(auctionId); }
}
