package com.example.daugia.service.impl;

import com.example.daugia.dto.AuthenticationRequest;
import com.example.daugia.dto.AuthenticationResponse;
import com.example.daugia.dto.LogoutRequest;
import com.example.daugia.dto.RefreshTokenRequest;
import com.example.daugia.dto.RegisterRequest;
import com.example.daugia.entity.Role;
import com.example.daugia.entity.User;
import com.example.daugia.exception.DuplicateResourceException;
import com.example.daugia.exception.InvalidTokenException;
import com.example.daugia.exception.ResourceNotFoundException;
import com.example.daugia.repository.RoleRepository;
import com.example.daugia.repository.UserRepository;
import com.example.daugia.security.JwtService;
import com.example.daugia.service.AuthenticationService;
import com.example.daugia.service.EmailService;
import com.example.daugia.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null) {
            request.getRoles().forEach(roleName -> {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                roles.add(role);
            });
        }

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(false)
                .build();
        var savedUser = repository.save(user);

        var jwtToken = jwtService.generateToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstname(), jwtToken);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                tokenBlacklistService.blacklistToken(refreshToken);

                var accessToken = jwtService.generateToken(user);
                var newRefreshToken = jwtService.generateRefreshToken(user);

                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }
        }
        throw new InvalidTokenException("Invalid refresh token");
    }

    @Override
    public void logout(LogoutRequest request) {
        String token = request.getToken();
        if (token != null) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    @Override
    public void verifyUser(String token) {
        String email = jwtService.extractUsername(token);
        if (email == null) {
            throw new InvalidTokenException("Invalid token");
        }
        User user = repository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (jwtService.isTokenValid(token, user)) {
            user.setEnabled(true);
            repository.save(user);
        } else {
            throw new InvalidTokenException("Invalid token");
        }
    }
}
