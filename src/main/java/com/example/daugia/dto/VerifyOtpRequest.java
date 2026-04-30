package com.example.daugia.dto;

import com.example.daugia.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {

    @NotBlank
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String otp;

    @NotNull
    private OtpPurpose purpose;
}