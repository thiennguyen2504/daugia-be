package com.example.daugia.auth.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
    void sendOtpEmail(String to, String name, String otp, String purpose);
    void sendAuctionApprovedEmail(String to, String sellerName, String productName, LocalDateTime startTime);
    void sendAuctionRejectedEmail(String to, String sellerName, String productName, String reason);
}
