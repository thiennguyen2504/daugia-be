package com.example.daugia.auth.service.impl;

import com.example.daugia.user.entity.Otp;
import com.example.daugia.user.entity.OtpPurpose;
import com.example.daugia.common.exception.InvalidTokenException;
import com.example.daugia.user.repository.OtpRepository;
import com.example.daugia.auth.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 10;

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String generateAndSaveOtp(String email, OtpPurpose purpose) {
        cleanupExpiredOtps(email, purpose);

        String code = generateOtpCode();
        Otp otp = Otp.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .used(false)
                .build();
        otpRepository.save(otp);
        return code;
    }

    @Override
    @Transactional
    public boolean validateOtp(String email, String code, OtpPurpose purpose) {
        Otp otp = otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new InvalidTokenException("OTP is invalid or has expired"));

        if (!otp.getCode().equals(code) || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("OTP is invalid or has expired");
        }

        otp.setUsed(true);
        otpRepository.save(otp);
        return true;
    }

    @Override
    @Transactional
    public void cleanupExpiredOtps(String email, OtpPurpose purpose) {
        otpRepository.deleteByEmailAndPurpose(email, purpose);
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt(bound));
    }
}
