package com.example.daugia.controller;

import com.example.daugia.dto.ApiResponse;
import com.example.daugia.dto.AuthenticationRequest;
import com.example.daugia.dto.AuthenticationResponse;
import com.example.daugia.dto.LogoutRequest;
import com.example.daugia.dto.RefreshTokenRequest;
import com.example.daugia.dto.RegisterRequest;
import com.example.daugia.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.ok(ApiResponse.success(
                "User registered successfully. Please check your email to verify.",
                null));
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

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyUser(@RequestParam("token") String token) {
        service.verifyUser(token);
        return ResponseEntity.ok(ApiResponse.success("Account verified successfully", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody @Valid LogoutRequest request) {
        service.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
