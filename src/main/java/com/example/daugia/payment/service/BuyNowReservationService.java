package com.example.daugia.payment.service;

import java.time.Duration;
import java.util.Optional;

public interface BuyNowReservationService {
    Optional<String> getReservationHolder(String auctionId);
    void createReservation(String auctionId, String bidderEmail, Duration ttl);
    void clearReservation(String auctionId);
    Optional<Long> getRemainingSeconds(String auctionId);
}
