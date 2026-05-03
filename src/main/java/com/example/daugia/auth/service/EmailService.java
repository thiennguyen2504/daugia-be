package com.example.daugia.auth.service;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
    void sendOtpEmail(String to, String name, String otp, String purpose);
}
