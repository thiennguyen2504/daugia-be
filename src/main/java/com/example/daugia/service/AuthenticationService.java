package com.example.daugia.service;

import com.example.daugia.dto.AuthenticationRequest;
import com.example.daugia.dto.AuthenticationResponse;
import com.example.daugia.dto.ChangePasswordRequest;
import com.example.daugia.dto.ForgotPasswordRequest;
import com.example.daugia.dto.LogoutRequest;
import com.example.daugia.dto.RefreshTokenRequest;
import com.example.daugia.dto.RegisterRequest;
import com.example.daugia.dto.ResetPasswordRequest;
import com.example.daugia.dto.VerifyOtpRequest;

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
