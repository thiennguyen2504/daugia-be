package com.example.daugia.auction.scheduler;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void activateApprovedAuctions() {
        List<Auction> ready = auctionRepository.findApprovedReadyToActivate(LocalDateTime.now());
        if (!ready.isEmpty()) {
            ready.forEach(a -> a.setStatus(AuctionStatus.ACTIVE));
            auctionRepository.saveAll(ready);
            log.info("[SCHEDULER] Activated {} auctions", ready.size());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void endActiveAuctions() {
        List<Auction> ended = auctionRepository.findActiveReadyToEnd(LocalDateTime.now());
        if (!ended.isEmpty()) {
            ended.forEach(a -> a.setStatus(AuctionStatus.ENDED));
            auctionRepository.saveAll(ended);
            log.info("[SCHEDULER] Ended {} auctions", ended.size());
        }
    }
}
