package com.example.daugia.bidding.service.impl;

import com.example.daugia.bidding.dto.BidHistoryEntryResponse;
import com.example.daugia.bidding.entity.BidHistoryEntry;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidHistoryRepository;
import com.example.daugia.bidding.service.BidHistoryService;
import com.example.daugia.bidding.util.EmailMaskingUtils;
import com.example.daugia.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidHistoryServiceImpl implements BidHistoryService {

    private final BidHistoryRepository bidHistoryRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(String auctionId, String bidderEmail, BigDecimal amount, BigDecimal increment, BidType bidType) {
        int nextStep = bidHistoryRepository.countByAuctionId(auctionId) + 1;

        bidHistoryRepository.save(BidHistoryEntry.builder()
                .auctionId(auctionId)
                .bidderEmailMasked(EmailMaskingUtils.mask(bidderEmail))
                .amount(amount)
                .bidIncrementApplied(increment)
                .stepNumber(nextStep)
                .bidType(bidType)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BidHistoryEntryResponse> getHistory(String auctionId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bidTime"));
        return PageResponse.from(bidHistoryRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable)
                .map(entry -> BidHistoryEntryResponse.builder()
                        .id(entry.getId())
                        .auctionId(entry.getAuctionId())
                        .bidderEmailMasked(entry.getBidderEmailMasked())
                        .amount(entry.getAmount())
                        .bidIncrementApplied(entry.getBidIncrementApplied())
                        .stepNumber(entry.getStepNumber())
                        .bidType(entry.getBidType())
                        .bidTime(entry.getBidTime())
                        .build()));
    }
}