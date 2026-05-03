package com.example.daugia.auth.service;

import com.example.daugia.auth.dto.AuthenticationRequest;
import com.example.daugia.auth.dto.AuthenticationResponse;
import com.example.daugia.auth.dto.ChangePasswordRequest;
import com.example.daugia.auth.dto.ForgotPasswordRequest;
import com.example.daugia.auth.dto.LogoutRequest;
import com.example.daugia.auth.dto.RefreshTokenRequest;
import com.example.daugia.auth.dto.RegisterRequest;
import com.example.daugia.auth.dto.ResetPasswordRequest;
import com.example.daugia.auth.dto.VerifyOtpRequest;

public interface AuthenticationService {
    void register(RegisterRequest request);

    AuthenticationResponse verifyRegistrationOtp(VerifyOtpRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void sendForgotPasswordOtp(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request, String currentUserEmail);
}
