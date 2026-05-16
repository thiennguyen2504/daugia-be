package com.example.daugia.bidding.service.impl;

import com.example.daugia.auction.config.AuctionProperties;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.entity.Bid;
import com.example.daugia.bidding.entity.BidStatus;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidRepository;
import com.example.daugia.bidding.service.BidExecutor;
import com.example.daugia.bidding.service.BidHistoryService;
import com.example.daugia.bidding.service.LeaderboardService;
import com.example.daugia.bidding.service.RedisBidPublisher;
import com.example.daugia.bidding.validator.BidValidator;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.event.AuctionEndedEvent;
import com.example.daugia.common.event.BidPlacedEvent;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidExecutorImpl implements BidExecutor {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final BidValidator bidValidator;
    private final BidHistoryService bidHistoryService;
    private final AuditService auditService;
    private final DomainEventPublisher eventPublisher;
    private final RedisBidPublisher redisBidPublisher;
    private final LeaderboardService leaderboardService;
    private final AuctionProperties auctionProperties;

    @Override
    @Transactional
    public BidResponse execute(Long auctionId, String bidderEmail, BigDecimal amount, BidType bidType) {
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));

        // Just-in-time activation if scheduler hasn't run yet
        if (auction.getStatus() == AuctionStatus.APPROVED &&
            !LocalDateTime.now().isBefore(auction.getBiddingStartTime())) {
            log.info("[JIT] Activating auction {} for bid", auctionId);
            auction.setStatus(AuctionStatus.ACTIVE);
        }

        var rejection = bidValidator.validate(auction, bidder, amount);
        if (rejection.isPresent()) {
            auditService.log(bidderEmail, AuditAction.BID_REJECTED, "AUCTION", String.valueOf(auctionId),
                AuditOutcome.FAILURE,
                AuditJsonUtils.toJson("auctionId", auctionId, "amount", amount, "rejectionReason", rejection.get().getRejectionReason()));
            return rejection.get();
        }

        Bid bid = bidRepository.save(Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(amount)
                .bidType(bidType)
                .status(BidStatus.WINNING)
                .build());
        bidHistoryService.record(auctionId, bidder.getEmail(), amount, auction.getBidIncrement(), bidType);

        bidRepository.findTopByAuctionIdAndStatusOrderByAmountDesc(auctionId, BidStatus.WINNING)
                .filter(previous -> !previous.getId().equals(bid.getId()))
                .ifPresent(previous -> previous.setStatus(BidStatus.OUTBID));

        auction.setCurrentPrice(amount);
        auction.setCurrentWinner(bidder);
        applyAntiSniping(auction);

        if (auction.getBuyNowPrice() != null && amount.compareTo(auction.getBuyNowPrice()) == 0) {
            auction.setStatus(AuctionStatus.ENDED);
            eventPublisher.publish(new AuctionEndedEvent(auction.getId(), bidder.getId(), amount,
                    auction.getProductName(),
                    auction.getSeller().getEmail(), auction.getSeller().getFullName(),
                    bidder.getEmail(), bidder.getFullName()));
        }

        eventPublisher.publish(new BidPlacedEvent(auctionId, bid.getId(), bidder.getId(), amount));
        auditService.log(bidderEmail, AuditAction.BID_PLACED, "BID", String.valueOf(bid.getId()),
            AuditOutcome.SUCCESS,
            AuditJsonUtils.toJson("auctionId", auctionId, "amount", amount));
        BidResponse response = toAcceptedResponse(auction, bid, bidder.getEmail());
        leaderboardService.updateLeaderboard(auctionId, bidder.getEmail(), amount, auction.getEndTime());
        redisBidPublisher.publish(response);

        return response;
    }

    private void applyAntiSniping(Auction auction) {
        long secondsRemaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (secondsRemaining <= auctionProperties.antinsiping().windowSeconds()) {
            auction.setEndTime(auction.getEndTime().plusSeconds(auctionProperties.antinsiping().extensionSeconds()));
            auction.setExtensionCount(auction.getExtensionCount() + 1);
        }
    }

    private BidResponse toAcceptedResponse(Auction auction, Bid bid, String winnerEmail) {
        return BidResponse.builder()
                .auctionId(auction.getId())
                .bidId(bid.getId())
                .amount(bid.getAmount())
                .currentPrice(auction.getCurrentPrice())
                .winnerEmail(winnerEmail)
                .endTime(auction.getEndTime())
                .status("ACCEPTED")
                .build();
    }
}
