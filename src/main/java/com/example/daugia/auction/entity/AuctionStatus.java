package com.example.daugia.auction.entity;

public enum AuctionStatus {
    PENDING,    // Awaiting admin review
    APPROVED,   // Approved, waiting for biddingStartTime
    ACTIVE,     // Bidding is live
    LIVE,       // Bidding is live (same as ACTIVE, can be used for clarity)
    ENDED,      // Bidding has closed
    REJECTED    // Rejected by admin (terminal state)
}
