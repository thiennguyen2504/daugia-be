package com.example.daugia.auction.scheduler;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
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

@Component
@ConditionalOnProperty(name = "auction.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final DomainEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "activateApprovedAuctions", lockAtMostFor = "PT55S", lockAtLeastFor = "PT10S")
    @Transactional
    public void activateApprovedAuctions() {
        List<Auction> ready = auctionRepository.findApprovedReadyToActivate(LocalDateTime.now());
        if (!ready.isEmpty()) {
            ready.forEach(a -> a.setStatus(AuctionStatus.ACTIVE));
            auctionRepository.saveAll(ready);
            log.info("[SCHEDULER] Activated {} auctions", ready.size());
        }
    }

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "endActiveAuctions", lockAtMostFor = "PT55S", lockAtLeastFor = "PT10S")
    @Transactional
    public void endActiveAuctions() {
        List<Auction> ended = auctionRepository.findActiveReadyToEnd(LocalDateTime.now());
        if (!ended.isEmpty()) {
            ended.forEach(a -> {
                a.setStatus(AuctionStatus.ENDED);
                var winner = a.getCurrentWinner();
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
            log.info("[SCHEDULER] Ended {} auctions", ended.size());
        }
    }
}
