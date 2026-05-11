package com.example.daugia.deposit.service;

import com.example.daugia.deposit.entity.Deposit;

import java.math.BigDecimal;

public interface DepositService {
    Deposit holdDeposit(Long auctionId, Long bidderId, BigDecimal amount);
    void releaseDeposit(Long auctionId, Long bidderId);
    void forfeitDeposit(Long auctionId, Long bidderId);
    void releaseAllNonWinners(Long auctionId, Long winnerId);
    boolean hasDeposit(Long auctionId, Long bidderId);
    BigDecimal getDepositAmount(Long auctionId);
}
