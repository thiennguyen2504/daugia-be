package com.example.daugia.bidding.service.impl;

import com.example.daugia.auction.config.AuctionProperties;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.service.BidHistoryService;
import com.example.daugia.bidding.entity.Bid;
import com.example.daugia.bidding.entity.BidStatus;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidRepository;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.bidding.service.BiddingService;
import com.example.daugia.bidding.service.LeaderboardService;
import com.example.daugia.bidding.service.RedisBidPublisher;
import com.example.daugia.bidding.util.EmailMaskingUtils;
import com.example.daugia.bidding.validator.BidValidator;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.bidding.validator.BidValidator;
import com.example.daugia.common.event.AuctionEndedEvent;
import com.example.daugia.common.event.BidPlacedEvent;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class BiddingServiceImpl implements BiddingService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final BidValidator bidValidator;
    private final BidHistoryService bidHistoryService;
    private final AuditService auditService;
    private final ObjectProvider<AutoBidService> autoBidServiceProvider;
    private final DomainEventPublisher eventPublisher;
    private final RedisBidPublisher redisBidPublisher;
    private final LeaderboardService leaderboardService;
    private final AuctionProperties auctionProperties;

    @Override
    @Transactional
    public BidResponse placeBid(Long auctionId, String bidderEmail, BigDecimal amount, BidType bidType) {
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));

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

        if (bidType == BidType.MANUAL) {
            triggerAutoBidProcessing(auctionId, bidder.getId());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidResponse> getBidHistory(Long auctionId, Pageable pageable) {
        return bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable)
                .map(bid -> BidResponse.builder()
                        .auctionId(auctionId)
                        .bidId(bid.getId())
                        .amount(bid.getAmount())
                        .currentPrice(bid.getAmount())
                        .winnerEmail(EmailMaskingUtils.mask(bid.getBidder().getEmail()))
                        .endTime(bid.getAuction().getEndTime())
                        .status(bid.getStatus().name())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public BidResponse getCurrentLeader(Long auctionId) {
        return bidRepository.findTopByAuctionIdAndStatusOrderByAmountDesc(auctionId, BidStatus.WINNING)
                .map(bid -> BidResponse.builder()
                        .auctionId(auctionId)
                        .bidId(bid.getId())
                        .amount(bid.getAmount())
                        .currentPrice(bid.getAuction().getCurrentPrice())
                        .winnerEmail(EmailMaskingUtils.mask(bid.getBidder().getEmail()))
                        .endTime(bid.getAuction().getEndTime())
                        .status(bid.getStatus().name())
                        .build())
                .orElseThrow(() -> new ResourceNotFoundException("No bids found"));
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

    private void triggerAutoBidProcessing(Long auctionId, Long bidderId) {
        Runnable task = () -> autoBidServiceProvider.getObject().processAutoBidsForAuction(auctionId, bidderId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }
}
