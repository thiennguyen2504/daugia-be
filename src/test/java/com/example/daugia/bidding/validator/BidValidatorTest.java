package com.example.daugia.bidding.validator;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BidValidatorTest {

    private BidValidator bidValidator;
    private Auction auction;
    private User bidder;

    @BeforeEach
    void setUp() {
        bidValidator = new BidValidator();
        auction = Auction.builder()
                .id("10")
                .status(AuctionStatus.ACTIVE)
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .endTime(LocalDateTime.now().plusHours(1))
                .seller(User.builder().id("99").build())
                .build();
        bidder = User.builder().id("2").email("bidder@test.com").build();
    }

    @Test
    void validateHappyPath() {
        var result = bidValidator.validate(auction, bidder, new BigDecimal("120.00"));

        assertThat(result).isEmpty();
    }

    @Test
    void validateBidTooLow() {
        var result = bidValidator.validate(auction, bidder, new BigDecimal("105.00"));

        assertThat(result).isPresent();
        assertThat(result.map(BidResponse::getRejectionReason)).contains("Bid too low");
    }

    @Test
    void validateAuctionNotActive() {
        auction.setStatus(AuctionStatus.APPROVED);

        var result = bidValidator.validate(auction, bidder, new BigDecimal("120.00"));

        assertThat(result).isPresent();
        assertThat(result.map(BidResponse::getRejectionReason)).contains("Auction not live");
    }

    @Test
    void validateAuctionEnded() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));

        var result = bidValidator.validate(auction, bidder, new BigDecimal("120.00"));

        assertThat(result).isPresent();
        assertThat(result.map(BidResponse::getRejectionReason)).contains("Auction has ended");
    }
}