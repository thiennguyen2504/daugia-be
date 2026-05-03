package com.example.daugia.auth.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.auth.dto.AuthenticationRequest;
import com.example.daugia.auth.dto.AuthenticationResponse;
import com.example.daugia.auth.dto.ChangePasswordRequest;
import com.example.daugia.auth.dto.ForgotPasswordRequest;
import com.example.daugia.auth.dto.LogoutRequest;
import com.example.daugia.auth.dto.RefreshTokenRequest;
import com.example.daugia.auth.dto.RegisterRequest;
import com.example.daugia.auth.dto.ResetPasswordRequest;
import com.example.daugia.auth.dto.VerifyOtpRequest;
import com.example.daugia.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @RequestBody @Valid RegisterRequest request) {
        service.register(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent to email", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> verifyRegistrationOtp(
            @RequestBody @Valid VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Registration verified successfully", service.verifyRegistrationOtp(request)));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Authenticated successfully", service.authenticate(request)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", service.refreshToken(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        service.sendForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent to email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        service.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        service.changePassword(request, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody @Valid LogoutRequest request) {
        service.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
