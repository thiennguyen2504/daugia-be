package com.example.daugia.service;

import com.example.daugia.dto.AuthenticationRequest;
import com.example.daugia.dto.AuthenticationResponse;
import com.example.daugia.dto.LogoutRequest;
import com.example.daugia.dto.RefreshTokenRequest;
import com.example.daugia.dto.RegisterRequest;

public interface AuthenticationService {
    void register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void verifyUser(String token);

    void logout(LogoutRequest request);
}
