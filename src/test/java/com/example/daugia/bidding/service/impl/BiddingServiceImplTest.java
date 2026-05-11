package com.example.daugia.bidding.service.impl;

import com.example.daugia.auction.config.AuctionProperties;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.bidding.entity.Bid;
import com.example.daugia.bidding.entity.BidStatus;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidRepository;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.bidding.service.BidHistoryService;
import com.example.daugia.bidding.service.LeaderboardService;
import com.example.daugia.bidding.service.RedisBidPublisher;
import com.example.daugia.bidding.validator.BidValidator;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.category.entity.Category;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.deposit.service.DepositService;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceImplTest {

    @Mock AuctionRepository auctionRepository;
    @Mock BidRepository bidRepository;
    @Mock UserRepository userRepository;
    @Mock DepositService depositService;
    @Mock AutoBidService autoBidService;
    @Mock ObjectProvider<AutoBidService> autoBidServiceProvider;
    @Mock BidHistoryService bidHistoryService;
    @Mock AuditService auditService;
    @Mock DomainEventPublisher eventPublisher;
    @Mock RedisBidPublisher redisBidPublisher;
    @Mock LeaderboardService leaderboardService;
    @Mock AuctionProperties auctionProperties;

    BiddingServiceImpl biddingService;

    private Auction auction;
    private User bidder;

    @BeforeEach
    void setUp() {
        lenient().when(autoBidServiceProvider.getObject()).thenReturn(autoBidService);

        BidValidator bidValidator = new BidValidator(depositService);
        biddingService = new BiddingServiceImpl(
            auctionRepository,
            bidRepository,
            userRepository,
            bidValidator,
                bidHistoryService,
                auditService,
            autoBidServiceProvider,
            eventPublisher,
            redisBidPublisher,
            leaderboardService,
            auctionProperties);

        User seller = User.builder().id(1L).email("seller@test.com").firstname("Sell").lastname("Er").build();
        bidder = User.builder().id(2L).email("bidder@test.com").firstname("Bid").lastname("Der").build();
        auction = Auction.builder()
                .id(10L)
                .productName("Phone")
                .description("Nice phone")
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .biddingStartTime(LocalDateTime.now().minusHours(1))
                .biddingEndTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .category(Category.builder().id(1L).name("Electronics").build())
                .build();
    }

    @Test
    void placeBidHappyPath() {
        mockCommonLookups(true);
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            bid.setId(99L);
            return bid;
        });
        when(bidRepository.findTopByAuctionIdAndStatusOrderByAmountDesc(10L, BidStatus.WINNING))
                .thenReturn(Optional.empty());
        when(auctionProperties.antinsiping()).thenReturn(new AuctionProperties.Antisnipe(60, 120));

        var response = biddingService.placeBid(10L, "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL);

        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("120.00");
        verify(redisBidPublisher).publish(response);
        verify(autoBidService).processAutoBidsForAuction(10L, 2L);
    }

    @Test
    void placeBidRejectsBidTooLow() {
        mockCommonLookups(true);

        var response = biddingService.placeBid(10L, "bidder@test.com", new BigDecimal("105.00"), BidType.MANUAL);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Bid too low");
        verifyNoInteractions(bidRepository);
    }

    @Test
    void placeBidRejectsAuctionNotLive() {
        auction.setStatus(AuctionStatus.APPROVED);
        mockCommonLookups(false);

        var response = biddingService.placeBid(10L, "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Auction not live");
    }

    @Test
    void placeBidRejectsNoDeposit() {
        mockCommonLookups(false);
        when(depositService.hasDeposit(10L, 2L)).thenReturn(false);

        var response = biddingService.placeBid(10L, "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Deposit required before bidding");
    }

    @Test
    void placeBidExtendsAuctionNearEndTime() {
        auction.setEndTime(LocalDateTime.now().plusSeconds(30));
        mockCommonLookups(true);
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            bid.setId(100L);
            return bid;
        });
        when(bidRepository.findTopByAuctionIdAndStatusOrderByAmountDesc(10L, BidStatus.WINNING))
                .thenReturn(Optional.empty());
        when(auctionProperties.antinsiping()).thenReturn(new AuctionProperties.Antisnipe(60, 120));

        var originalEndTime = auction.getEndTime();
        var response = biddingService.placeBid(10L, "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL);

        assertThat(response.getEndTime()).isAfter(originalEndTime);
        assertThat(auction.getExtensionCount()).isEqualTo(1);
    }

    private void mockCommonLookups(boolean includeDeposit) {
        when(auctionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(auction));
        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.of(bidder));
        if (includeDeposit) {
            when(depositService.hasDeposit(10L, 2L)).thenReturn(true);
        }
    }
}
