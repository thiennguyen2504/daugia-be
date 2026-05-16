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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "activateApprovedAuctions", lockAtMostFor = "PT55S", lockAtLeastFor = "PT10S")
    @Transactional
    public void activateApprovedAuctions() {
        log.debug("[SCHEDULER] Checking for auctions to activate...");
        List<Auction> ready = auctionRepository.findApprovedReadyToActivate(LocalDateTime.now());
        if (!ready.isEmpty()) {
            ready.forEach(a -> {
                a.setStatus(AuctionStatus.ACTIVE);
                auditService.log("SCHEDULER", AuditAction.AUCTION_ACTIVATED, "AUCTION", a.getId(),
                        AuditOutcome.SUCCESS, AuditJsonUtils.toJson("title", a.getProductName()));
            });
            auctionRepository.saveAll(ready);
            List<String> ids = ready.stream().map(Auction::getId).collect(Collectors.toList());
            log.info("[SCHEDULER] Activated {} auctions: {}", ready.size(), ids);
        }
    }

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "endActiveAuctions", lockAtMostFor = "PT55S", lockAtLeastFor = "PT10S")
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
            });
            auctionRepository.saveAll(ended);
            List<String> ids = ended.stream().map(Auction::getId).collect(Collectors.toList());
            log.info("[SCHEDULER] Ended {} auctions: {}", ended.size(), ids);
        }
    }
}
