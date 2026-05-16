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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AutoBidServiceImpl implements AutoBidService {

    private final AutoBidConfigRepository autoBidConfigRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidExecutor bidExecutor;

    @Override
    @Transactional
    public AutoBidConfigResponse upsertConfig(Long auctionId, String bidderEmail, BigDecimal maxAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));
        AutoBidConfig config = autoBidConfigRepository.findByAuctionIdAndBidderEmail(auctionId, bidderEmail)
                .orElseGet(() -> AutoBidConfig.builder().auction(auction).bidder(bidder).build());
        config.setMaxAmount(maxAmount);
        config.setActive(true);
        return toResponse(autoBidConfigRepository.save(config));
    }

    @Override
    @Transactional
    public void deactivateConfig(Long auctionId, String bidderEmail) {
        AutoBidConfig config = autoBidConfigRepository.findByAuctionIdAndBidderEmail(auctionId, bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Auto-bid config not found"));
        config.setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public AutoBidConfigResponse getOwnConfig(Long auctionId, String bidderEmail) {
        return autoBidConfigRepository.findByAuctionIdAndBidderEmail(auctionId, bidderEmail)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Auto-bid config not found"));
    }

    @Override
    @Async("domainEventExecutor")
    @Transactional
    public void processAutoBidsForAuction(Long auctionId, Long excludeBidderId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        BigDecimal currentPrice = auction.getCurrentPrice() == null ? auction.getStartingPrice() : auction.getCurrentPrice();
        autoBidConfigRepository
                .findAllByAuctionIdAndActiveTrueAndBidderIdNotAndMaxAmountGreaterThanOrderByMaxAmountDesc(
                        auctionId, excludeBidderId, currentPrice)
                .stream()
                .findFirst()
                .ifPresent(config -> {
                    BigDecimal nextBid = currentPrice.add(auction.getBidIncrement()).min(config.getMaxAmount());
                    if (nextBid.compareTo(currentPrice) > 0) {
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
