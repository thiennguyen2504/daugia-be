package com.example.daugia.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AutoBidConfigRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal maxAmount;
}
