package com.example.daugia.bidding.service.impl;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.bidding.dto.AutoBidConfigResponse;
import com.example.daugia.bidding.entity.AutoBidConfig;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.AutoBidConfigRepository;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.bidding.service.BidExecutor;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditJsonUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoBidServiceImpl implements AutoBidService {

    private final AutoBidConfigRepository autoBidConfigRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidExecutor bidExecutor;
        private final AuditService auditService;

    @Override
    @Transactional
    public AutoBidConfigResponse upsertConfig(String auctionId, String bidderEmail, BigDecimal maxAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));
        AutoBidConfig config = autoBidConfigRepository.findByAuctionIdAndBidderEmail(auctionId, bidderEmail)
                .orElseGet(() -> AutoBidConfig.builder().auction(auction).bidder(bidder).build());
        config.setMaxAmount(maxAmount);
        config.setActive(true);
        config = autoBidConfigRepository.save(config);
        auditService.log(bidderEmail, AuditAction.AUTO_BID_CONFIGURED, "AUTO_BID_CONFIG", config.getId(),
                AuditOutcome.SUCCESS,
                AuditJsonUtils.toJson("auctionId", auctionId, "maxAmount", maxAmount));
        return toResponse(config);
    }

    @Override
    @Transactional
    public void deactivateConfig(String auctionId, String bidderEmail) {
        AutoBidConfig config = autoBidConfigRepository.findByAuctionIdAndBidderEmail(auctionId, bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Auto-bid config not found"));
        config.setActive(false);
        auditService.log(bidderEmail, AuditAction.AUTO_BID_DEACTIVATED, "AUTO_BID_CONFIG", config.getId(),
                AuditOutcome.SUCCESS,
                AuditJsonUtils.toJson("auctionId", auctionId));
    }

    @Override
    @Transactional(readOnly = true)
    public AutoBidConfigResponse getOwnConfig(String auctionId, String bidderEmail) {
        return autoBidConfigRepository.findByAuctionIdAndBidderEmail(auctionId, bidderEmail)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Auto-bid config not found"));
    }

    @Override
    @Async("domainEventExecutor")
    @Transactional
    public void processAutoBidsForAuction(String auctionId, String excludeBidderId) {
        log.debug("Processing auto-bids for auctionId={}, excludeBidderId={}", auctionId, excludeBidderId);
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        BigDecimal currentPrice = auction.getCurrentPrice() == null ? auction.getStartingPrice() : auction.getCurrentPrice();
        
        var configs = autoBidConfigRepository
                .findAllByAuctionIdAndActiveTrueAndBidderIdNotAndMaxAmountGreaterThanOrderByMaxAmountDesc(
                        auctionId, excludeBidderId, currentPrice);
                        
        if (configs.isEmpty()) {
            log.debug("No eligible auto-bid found for auctionId={}", auctionId);
            return;
        }
        
        configs.stream()
                .findFirst()
                .ifPresent(config -> {
                    BigDecimal nextBid = currentPrice.add(auction.getBidIncrement()).min(config.getMaxAmount());
                    if (nextBid.compareTo(currentPrice) > 0) {
                        log.info("Auto-bid fired: auctionId={} autoBidderEmail={} nextBid={}", auctionId, config.getBidder().getEmail(), nextBid);
                        bidExecutor.execute(auctionId, config.getBidder().getEmail(), nextBid, BidType.AUTO);
                    }
                });
    }

    private AutoBidConfigResponse toResponse(AutoBidConfig config) {
        return AutoBidConfigResponse.builder()
                .auctionId(config.getAuction().getId())
                .maxAmount(config.getMaxAmount())
                .active(config.isActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
