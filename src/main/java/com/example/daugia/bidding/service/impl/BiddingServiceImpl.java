package com.example.daugia.bidding.service.impl;

import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.entity.BidStatus;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidRepository;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.bidding.service.BidExecutor;
import com.example.daugia.bidding.service.BiddingService;
import com.example.daugia.bidding.util.EmailMaskingUtils;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BiddingServiceImpl implements BiddingService {

    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final BidExecutor bidExecutor;
    private final AutoBidService autoBidService;

    @Override
    @Transactional
    public BidResponse placeBid(String auctionId, String bidderEmail, BigDecimal amount, BidType bidType) {
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));
        BidResponse response = bidExecutor.execute(auctionId, bidderEmail, amount, bidType);
        if (bidType == BidType.MANUAL) {
            triggerAutoBidProcessing(auctionId, bidder.getId());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidResponse> getBidHistory(String auctionId, Pageable pageable) {
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
    public BidResponse getCurrentLeader(String auctionId) {
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

    private void triggerAutoBidProcessing(String auctionId, String bidderId) {
        Runnable task = () -> autoBidService.processAutoBidsForAuction(auctionId, bidderId);
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
