package com.example.daugia.common.event;

import com.example.daugia.auth.service.EmailService;
import com.example.daugia.deposit.service.DepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@Async("domainEventExecutor")
@RequiredArgsConstructor
public class DomainEventListener {

    private final EmailService emailService;
    private final DepositService depositService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(CategoryCreatedEvent event) {
        log.info("[EVENT] CategoryCreated — id={}, name={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
        // TODO: trigger cache invalidation, search index update
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryUpdated(CategoryUpdatedEvent event) {
        log.info("[EVENT] CategoryUpdated — id={}, newName={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryDeleted(CategoryDeletedEvent event) {
        log.info("[EVENT] CategoryDeleted — id={}, name={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionCreated(AuctionCreatedEvent event) {
        log.info("[EVENT] AuctionCreated — id={}, product={}, seller={}, at={}",
                event.getAggregateId(), event.getProductName(), event.getSellerEmail(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionApproved(AuctionApprovedEvent event) {
        log.info("[EVENT] AuctionApproved — id={}, product={}", event.getAggregateId(), event.getProductName());
        emailService.sendAuctionApprovedEmail(
                event.getSellerEmail(), event.getSellerName(),
                event.getProductName(), event.getBiddingStartTime());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionRejected(AuctionRejectedEvent event) {
        log.info("[EVENT] AuctionRejected — id={}, product={}, reason={}",
                event.getAggregateId(), event.getProductName(), event.getRejectionReason());
        emailService.sendAuctionRejectedEmail(
                event.getSellerEmail(), event.getSellerName(),
                event.getProductName(), event.getRejectionReason());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepositHeld(DepositHeldEvent event) {
        log.info("[EVENT] DepositHeld — auction={}, bidder={}, amount={}",
                event.getAuctionId(), event.getBidderId(), event.getAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepositReleased(DepositReleasedEvent event) {
        log.info("[EVENT] DepositReleased — auction={}, bidder={}", event.getAuctionId(), event.getBidderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepositForfeited(DepositForfeitedEvent event) {
        log.info("[EVENT] DepositForfeited — auction={}, bidder={}", event.getAuctionId(), event.getBidderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBidPlaced(BidPlacedEvent event) {
        log.info("[EVENT] BidPlaced — auction={}, bid={}, bidder={}, amount={}",
                event.getAuctionId(), event.getBidId(), event.getBidderId(), event.getAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEnded(AuctionEndedEvent event) {
        depositService.releaseAllNonWinners(event.getAuctionId(), event.getWinnerId());

        if (event.getWinnerEmail() != null && event.getWinnerName() != null) {
            emailService.sendAuctionWinnerEmail(event.getWinnerEmail(), event.getWinnerName(), event.getProductName());
        }
        emailService.sendAuctionSoldEmail(event.getSellerEmail(), event.getSellerName(), event.getProductName());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[EVENT] PaymentCompleted — auction={}, payer={}, amount={}",
                event.getAuctionId(), event.getPayerEmail(), event.getAmount());
    }
}
