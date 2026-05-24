package com.example.daugia.payment.dto;

import lombok.Builder;

@Builder
public record BuyNowReservationStatusResponse(
    boolean hasReservation,
    boolean isOwner,
    Long remainingSeconds,
    String paymentUrl
) {
}
