package com.example.daugia.payment.dto;

import com.example.daugia.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String auctionId;
    private String payerEmail;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentUrl;
    private String vnpayTransactionNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

