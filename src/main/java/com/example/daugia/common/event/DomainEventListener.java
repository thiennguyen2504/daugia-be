package com.example.daugia.common.event;

import com.example.daugia.auth.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DomainEventListener {

    @Autowired
    private EmailService emailService;

    @EventListener
    public void onCategoryCreated(CategoryCreatedEvent event) {
        log.info("[EVENT] CategoryCreated — id={}, name={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
        // TODO: trigger cache invalidation, search index update
    }

    @EventListener
    public void onCategoryUpdated(CategoryUpdatedEvent event) {
        log.info("[EVENT] CategoryUpdated — id={}, newName={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @EventListener
    public void onCategoryDeleted(CategoryDeletedEvent event) {
        log.info("[EVENT] CategoryDeleted — id={}, name={}, at={}",
                event.getAggregateId(), event.getCategoryName(), event.getOccurredAt());
    }

    @EventListener
    public void onAuctionCreated(AuctionCreatedEvent event) {
        log.info("[EVENT] AuctionCreated — id={}, product={}, seller={}, at={}",
                event.getAggregateId(), event.getProductName(), event.getSellerEmail(), event.getOccurredAt());
    }

    @EventListener
    public void onAuctionApproved(AuctionApprovedEvent event) {
        log.info("[EVENT] AuctionApproved — id={}, product={}", event.getAggregateId(), event.getProductName());
        emailService.sendAuctionApprovedEmail(
                event.getSellerEmail(), event.getSellerName(),
                event.getProductName(), event.getBiddingStartTime());
    }

    @EventListener
    public void onAuctionRejected(AuctionRejectedEvent event) {
        log.info("[EVENT] AuctionRejected — id={}, product={}, reason={}",
                event.getAggregateId(), event.getProductName(), event.getRejectionReason());
        emailService.sendAuctionRejectedEmail(
                event.getSellerEmail(), event.getSellerName(),
                event.getProductName(), event.getRejectionReason());
    }
}
