package com.example.daugia.deposit.service.impl;

import com.example.daugia.auction.config.AuctionProperties;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.common.event.DepositForfeitedEvent;
import com.example.daugia.common.event.DepositHeldEvent;
import com.example.daugia.common.event.DepositReleasedEvent;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.exception.DuplicateResourceException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.deposit.entity.Deposit;
import com.example.daugia.deposit.entity.DepositStatus;
import com.example.daugia.deposit.repository.DepositRepository;
import com.example.daugia.deposit.service.DepositService;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositServiceImpl implements DepositService {

    private final DepositRepository depositRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuctionProperties auctionProperties;
    private final DomainEventPublisher eventPublisher;
    private final AuditService auditService;

    @Override
    @Transactional
    public Deposit holdDeposit(Long auctionId, Long bidderId, BigDecimal amount) {
        if (hasDeposit(auctionId, bidderId)) {
            throw new DuplicateResourceException("Deposit already held for this auction");
        }
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));
        BigDecimal requiredAmount = amount == null ? getDepositAmount(auctionId) : amount;

        log.info("Holding deposit amount={} for auction={} bidder={}", requiredAmount, auctionId, bidderId);
        Deposit deposit = depositRepository.save(Deposit.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(requiredAmount)
                .status(DepositStatus.HELD)
                .heldAt(LocalDateTime.now())
                .build());
        eventPublisher.publish(new DepositHeldEvent(auctionId, bidderId, requiredAmount));
        auditService.log(bidder.getEmail(), AuditAction.DEPOSIT_HELD, "DEPOSIT", String.valueOf(deposit.getId()),
            AuditOutcome.SUCCESS,
            AuditJsonUtils.toJson("auctionId", auctionId, "amount", requiredAmount));
        return deposit;
    }

    @Override
    @Transactional
    public void releaseDeposit(Long auctionId, Long bidderId) {
        Deposit deposit = depositRepository.findByAuctionIdAndBidderId(auctionId, bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found"));
        deposit.setStatus(DepositStatus.RELEASED);
        deposit.setReleasedAt(LocalDateTime.now());
        eventPublisher.publish(new DepositReleasedEvent(auctionId, bidderId));
        auditService.log(deposit.getBidder().getEmail(), AuditAction.DEPOSIT_RELEASED, "DEPOSIT", String.valueOf(deposit.getId()),
            AuditOutcome.SUCCESS,
            AuditJsonUtils.toJson("auctionId", auctionId, "bidderId", bidderId));
    }

    @Override
    @Transactional
    public void releaseAllNonWinners(Long auctionId, Long winnerId) {
        depositRepository.findAllByAuctionIdAndStatus(auctionId, DepositStatus.HELD).stream()
                .filter(deposit -> winnerId == null || !winnerId.equals(deposit.getBidder().getId()))
                .forEach(deposit -> releaseDeposit(auctionId, deposit.getBidder().getId()));
    }

    @Override
    @Transactional
    public void forfeitDeposit(Long auctionId, Long bidderId) {
        Deposit deposit = depositRepository.findByAuctionIdAndBidderId(auctionId, bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found"));
        deposit.setStatus(DepositStatus.FORFEITED);
        eventPublisher.publish(new DepositForfeitedEvent(auctionId, bidderId));
        auditService.log(deposit.getBidder().getEmail(), AuditAction.DEPOSIT_FORFEITED, "DEPOSIT", String.valueOf(deposit.getId()),
            AuditOutcome.SUCCESS,
            AuditJsonUtils.toJson("auctionId", auctionId, "bidderId", bidderId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasDeposit(Long auctionId, Long bidderId) {
        return depositRepository.existsByAuctionIdAndBidderIdAndStatus(auctionId, bidderId, DepositStatus.HELD);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDepositAmount(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        return auction.getStartingPrice().multiply(auctionProperties.deposit().ratio());
    }
}
