package com.example.daugia.auth.service.impl;

import com.example.daugia.auth.dto.AuthenticationRequest;
import com.example.daugia.auth.dto.AuthenticationResponse;
import com.example.daugia.auth.dto.ChangePasswordRequest;
import com.example.daugia.auth.dto.ForgotPasswordRequest;
import com.example.daugia.auth.dto.LogoutRequest;
import com.example.daugia.auth.dto.RefreshTokenRequest;
import com.example.daugia.auth.dto.RegisterRequest;
import com.example.daugia.auth.dto.ResetPasswordRequest;
import com.example.daugia.auth.dto.VerifyOtpRequest;
import com.example.daugia.user.entity.OtpPurpose;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.RoleType;
import com.example.daugia.user.entity.User;
import com.example.daugia.common.exception.AppException;
import com.example.daugia.common.exception.DuplicateResourceException;
import com.example.daugia.common.exception.InvalidTokenException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.repository.RoleRepository;
import com.example.daugia.user.repository.UserRepository;
import com.example.daugia.auth.security.JwtService;
import com.example.daugia.auth.service.AuthenticationService;
import com.example.daugia.auth.service.EmailService;
import com.example.daugia.auth.service.OtpService;
import com.example.daugia.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        if (repository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        String[] nameParts = request.getFullName().trim().split("\\s+");
        String firstname = nameParts[0];
        String lastname = nameParts.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length))
                : "";

        Role role = roleRepository.findByName(request.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole().name()));

        User user = User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(false)
                .build();

        repository.save(user);

        String otp = otpService.generateAndSaveOtp(request.getEmail(), OtpPurpose.REGISTRATION);
        emailService.sendOtpEmail(request.getEmail(), firstname, otp, "account verification");
    }

    @Override
    @Transactional
    public AuthenticationResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        otpService.validateOtp(request.getEmail(), request.getOtp(), request.getPurpose());

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(true);
        repository.save(user);

        return buildAuthenticationResponse(user);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = repository.findByEmail(request.getIdentifier())
                .or(() -> repository.findByPhone(request.getIdentifier()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new AppException("Account is not verified. Please verify your OTP.", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return buildAuthenticationResponse(user);
    }

    @Override
    @Transactional
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
                        .role(user.getRole().getName())
                        .build();
            }
        }
        throw new InvalidTokenException("Invalid refresh token");
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String token = request.getToken();
        if (token != null) {
            tokenBlacklistService.blacklistToken(token);
        }
    }

    @Override
    @Transactional
    public void sendForgotPasswordOtp(ForgotPasswordRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpPurpose.FORGOT_PASSWORD);
        emailService.sendOtpEmail(user.getEmail(), user.getFirstname(), otp, "password reset");
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        otpService.validateOtp(request.getEmail(), request.getOtp(), OtpPurpose.FORGOT_PASSWORD);

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String currentUserEmail) {
        User user = repository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException("New password must be different from current password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    private AuthenticationResponse buildAuthenticationResponse(User user) {
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .role(user.getRole().getName())
                .build();
    }
}
