package com.example.daugia.common.event;

public class AuctionRejectedEvent extends DomainEvent {

    private final String auctionId;
    private final String productName;
    private final String sellerEmail;
    private final String sellerName;
    private final String rejectionReason;

    public AuctionRejectedEvent(String auctionId, String productName,
                                String sellerEmail, String sellerName,
                                String rejectionReason) {
        this.auctionId       = auctionId;
        this.productName     = productName;
        this.sellerEmail     = sellerEmail;
        this.sellerName      = sellerName;
        this.rejectionReason = rejectionReason;
    }

    public String getAuctionId()       { return auctionId; }
    public String getProductName()   { return productName; }
    public String getSellerEmail()   { return sellerEmail; }
    public String getSellerName()    { return sellerName; }
    public String getRejectionReason() { return rejectionReason; }

    @Override public String getAggregateType() { return "AUCTION"; }
    @Override public String getAggregateId()   { return auctionId; }
}
