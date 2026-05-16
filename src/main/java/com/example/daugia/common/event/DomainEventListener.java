package com.example.daugia.common.event;

import com.example.daugia.auth.service.EmailService;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(CategoryCreatedEvent event) {
        log.info("[EVENT] CategoryCreated — eventId={}, id={}, name={}, at={}",
                event.getEventId(), event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
        // TODO: trigger cache invalidation, search index update
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryUpdated(CategoryUpdatedEvent event) {
        log.info("[EVENT] CategoryUpdated — eventId={}, id={}, newName={}, at={}",
                event.getEventId(), event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryDeleted(CategoryDeletedEvent event) {
        log.info("[EVENT] CategoryDeleted — eventId={}, id={}, name={}, at={}",
                event.getEventId(), event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionCreated(AuctionCreatedEvent event) {
        log.info("[EVENT] AuctionCreated — eventId={}, id={}, product={}, seller={}, at={}",
                event.getEventId(), event.getAggregateId(), event.getProductName(), event.getSellerEmail(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionApproved(AuctionApprovedEvent event) {
        log.info("[EVENT] AuctionApproved — eventId={}, id={}, product={}, at={}", event.getEventId(), event.getAggregateId(), event.getProductName(), event.getOccurredAt());
        emailService.sendAuctionApprovedEmail(
                event.getSellerEmail(), event.getSellerName(),
                event.getProductName(), event.getBiddingStartTime());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionRejected(AuctionRejectedEvent event) {
        log.info("[EVENT] AuctionRejected — eventId={}, id={}, product={}, reason={}, at={}",
                event.getEventId(), event.getAggregateId(), event.getProductName(), event.getRejectionReason(), event.getOccurredAt());
        emailService.sendAuctionRejectedEmail(
                event.getSellerEmail(), event.getSellerName(),
                event.getProductName(), event.getRejectionReason());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBidPlaced(BidPlacedEvent event) {
        log.info("[EVENT] BidPlaced — eventId={}, auction={}, bid={}, bidder={}, amount={}, at={}",
                event.getEventId(), event.getAuctionId(), event.getBidId(), event.getBidderId(), event.getAmount(), event.getOccurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEnded(AuctionEndedEvent event) {
        log.info("[EVENT] AuctionEnded — eventId={}, auction={}, product={}, winner={}, at={}",
                event.getEventId(), event.getAuctionId(), event.getProductName(), event.getWinnerEmail(), event.getOccurredAt());
        if (event.getWinnerEmail() != null && event.getWinnerName() != null) {
            emailService.sendAuctionWinnerEmail(event.getWinnerEmail(), event.getWinnerName(), event.getProductName());
        }
        emailService.sendAuctionSoldEmail(event.getSellerEmail(), event.getSellerName(), event.getProductName());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[EVENT] PaymentCompleted — eventId={}, auction={}, payer={}, amount={}, at={}",
                event.getEventId(), event.getAuctionId(), event.getPayerEmail(), event.getAmount(), event.getOccurredAt());
    }
}
