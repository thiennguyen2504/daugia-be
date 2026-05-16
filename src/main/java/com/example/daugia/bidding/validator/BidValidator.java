package com.example.daugia.bidding.validator;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.deposit.service.DepositService;
import com.example.daugia.user.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class BidValidator {

    private final DepositService depositService;

    public BidValidator(DepositService depositService) {
        this.depositService = depositService;
    }

    public Optional<BidResponse> validate(Auction auction, User bidder, BigDecimal amount) {
        if (bidder.getId().equals(auction.getSeller().getId())) {
            return Optional.of(rejected(auction, "Seller cannot bid on their own auction"));
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            return Optional.of(rejected(auction, "Auction not live"));
        }
        if (auction.getEndTime() == null || !LocalDateTime.now().isBefore(auction.getEndTime())) {
            return Optional.of(rejected(auction, "Auction has ended"));
        }
        if (!depositService.hasDeposit(auction.getId(), bidder.getId())) {
            return Optional.of(rejected(auction, "Deposit required before bidding"));
        }

        BigDecimal currentPrice = auction.getCurrentPrice() == null ? auction.getStartingPrice() : auction.getCurrentPrice();
        if (amount.compareTo(currentPrice.add(auction.getBidIncrement())) < 0) {
            return Optional.of(rejected(auction, "Bid too low"));
        }
        if (auction.getBuyNowPrice() != null && amount.compareTo(auction.getBuyNowPrice()) > 0) {
            return Optional.of(rejected(auction, "Bid exceeds buy now price"));
        }

        return Optional.empty();
    }

    private BidResponse rejected(Auction auction, String reason) {
        return BidResponse.builder()
                .auctionId(auction.getId())
                .currentPrice(auction.getCurrentPrice())
                .endTime(auction.getEndTime())
                .status("REJECTED")
                .rejectionReason(reason)
                .build();
    }
}
