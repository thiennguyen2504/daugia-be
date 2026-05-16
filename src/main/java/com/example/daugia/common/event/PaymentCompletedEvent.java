package com.example.daugia.common.event;

import java.math.BigDecimal;

public class PaymentCompletedEvent extends DomainEvent {
    private final Long auctionId;
    private final String payerEmail;
    private final BigDecimal amount;

    public PaymentCompletedEvent(Long auctionId, String payerEmail, BigDecimal amount) {
        this.auctionId = auctionId;
        this.payerEmail = payerEmail;
        this.amount = amount;
    }

    public Long getAuctionId() { return auctionId; }
    public String getPayerEmail() { return payerEmail; }
    public BigDecimal getAmount() { return amount; }

    @Override public String getAggregateType() { return "PAYMENT"; }
    @Override public String getAggregateId() { return String.valueOf(auctionId); }
}
