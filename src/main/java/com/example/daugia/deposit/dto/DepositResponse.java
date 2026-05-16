package com.example.daugia.deposit.dto;

import com.example.daugia.deposit.entity.DepositStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepositResponse {
    private Long id;
    private Long auctionId;
    private String productName;
    private Long bidderId;
    private String bidderEmail;
    private BigDecimal amount;
    private DepositStatus status;
    private LocalDateTime heldAt;
    private LocalDateTime releasedAt;
}
