package com.example.daugia.auction.scheduler;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.event.AuctionEndedEvent;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.payment.entity.Payment;
import com.example.daugia.payment.entity.PaymentStatus;
import com.example.daugia.payment.repository.PaymentRepository;
import com.example.daugia.payment.service.BuyNowReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "auction.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final DomainEventPublisher eventPublisher;
    private final AuditService auditService;
    private final CacheManager cacheManager;
    private final PaymentRepository paymentRepository;
    private final BuyNowReservationService buyNowReservationService;

    @Scheduled(fixedRate = 10_000)
    @SchedulerLock(name = "activateApprovedAuctions", lockAtMostFor = "PT9S", lockAtLeastFor = "PT2S")
    @Transactional
    public void activateApprovedAuctions() {
        log.debug("[SCHEDULER] Checking for auctions to activate...");
        List<Auction> ready = auctionRepository.findApprovedReadyToActivate(LocalDateTime.now());
        if (!ready.isEmpty()) {
            ready.forEach(a -> {
                a.setStatus(AuctionStatus.ACTIVE);
                auditService.log("SCHEDULER", AuditAction.AUCTION_ACTIVATED, "AUCTION", a.getId(),
                        AuditOutcome.SUCCESS, AuditJsonUtils.toJson("title", a.getProductName()));
                var cache = cacheManager.getCache("auctions");
                if (cache != null) {
                    cache.evictIfPresent(a.getId() + "-public");
                }
            });
            auctionRepository.saveAll(ready);
            List<String> ids = ready.stream().map(Auction::getId).collect(Collectors.toList());
            log.info("[SCHEDULER] Activated {} auctions: {}", ready.size(), ids);
        }
    }

    @Scheduled(fixedRate = 10_000)
    @SchedulerLock(name = "endActiveAuctions", lockAtMostFor = "PT9S", lockAtLeastFor = "PT2S")
    @Transactional
    public void endActiveAuctions() {
        log.debug("[SCHEDULER] Checking for auctions to end...");
        List<Auction> ended = auctionRepository.findActiveReadyToEnd(LocalDateTime.now());
        if (!ended.isEmpty()) {
            ended.forEach(a -> {
                a.setStatus(AuctionStatus.ENDED);
                var winner = a.getCurrentWinner();
                if (winner == null) {
                    log.warn("[SCHEDULER] Auction ended with NO WINNER: auctionId={}", a.getId());
                }
                auditService.log("SCHEDULER", AuditAction.AUCTION_ENDED, "AUCTION", a.getId(),
                        AuditOutcome.SUCCESS, AuditJsonUtils.toJson("hasWinner", winner != null, "finalPrice", a.getCurrentPrice()));
                
                eventPublisher.publish(new AuctionEndedEvent(
                        a.getId(),
                    winner == null ? null : winner.getId(),
                        a.getCurrentPrice(),
                    a.getProductName(),
                        a.getSeller().getEmail(),
                    a.getSeller().getFullName(),
                    winner == null ? null : winner.getEmail(),
                    winner == null ? null : winner.getFullName()));
                var cache = cacheManager.getCache("auctions");
                if (cache != null) {
                    cache.evictIfPresent(a.getId() + "-public");
                }
            });
            auctionRepository.saveAll(ended);
            List<String> ids = ended.stream().map(Auction::getId).collect(Collectors.toList());
            log.info("[SCHEDULER] Ended {} auctions: {}", ended.size(), ids);
        }
    }

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "cleanExpiredReservations", lockAtMostFor = "PT59S", lockAtLeastFor = "PT2S")
    @Transactional
    public void cleanExpiredReservations() {
        log.debug("[SCHEDULER] Checking for expired Buy Now reservations...");
        // Get all pending payments
        List<Payment> pendingPayments = paymentRepository.findAllByStatus(PaymentStatus.PENDING);
        for (Payment payment : pendingPayments) {
            Auction auction = payment.getAuction();
            if (auction.getStatus() == AuctionStatus.ACTIVE || auction.getStatus() == AuctionStatus.LIVE) {
                if (buyNowReservationService.getReservationHolder(auction.getId()).isEmpty()) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    auditService.log("SCHEDULER", AuditAction.PAYMENT_FAILED, "PAYMENT", payment.getId(),
                            AuditOutcome.FAILURE, AuditJsonUtils.toJson("reason", "Reservation expired"));
                    log.info("[SCHEDULER] Marked pending payment {} as FAILED due to expired reservation", payment.getId());
                }
            }
        }
    }
}
