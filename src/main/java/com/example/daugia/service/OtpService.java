package com.example.daugia.service;

import com.example.daugia.entity.OtpPurpose;

public interface OtpService {
    String generateAndSaveOtp(String email, OtpPurpose purpose);

    boolean validateOtp(String email, String code, OtpPurpose purpose);

    void cleanupExpiredOtps(String email, OtpPurpose purpose);
}