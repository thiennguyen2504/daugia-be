package com.example.daugia.payment.service;

import com.example.daugia.payment.dto.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentService {
    String createPaymentUrl(Long auctionId, String winnerEmail, HttpServletRequest request);
    PaymentResponse handleCallback(Map<String, String> params);
    PaymentResponse getByAuction(Long auctionId, String userEmail);
}
