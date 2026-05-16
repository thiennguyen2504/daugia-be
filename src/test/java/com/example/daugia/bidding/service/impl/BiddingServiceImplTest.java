package com.example.daugia.bidding.service.impl;

import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidRepository;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.bidding.service.BidExecutor;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceImplTest {

    @Mock BidRepository bidRepository;
    @Mock UserRepository userRepository;
    @Mock BidExecutor bidExecutor;
    @Mock AutoBidService autoBidService;

    BiddingServiceImpl biddingService;

    private User bidder;
    private BidResponse mockResponse;

    @BeforeEach
    void setUp() {
        biddingService = new BiddingServiceImpl(
            bidRepository,
            userRepository,
            bidExecutor,
            autoBidService
        );

        bidder = User.builder().id("2").email("bidder@test.com").firstname("Bid").lastname("Der").build();
        mockResponse = BidResponse.builder().status("ACCEPTED").currentPrice(new BigDecimal("120.00")).build();
    }

    @Test
    void placeBidHappyPath() {
        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.of(bidder));
        when(bidExecutor.execute("10", "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL))
            .thenReturn(mockResponse);

        // Since we are not running within an actual Spring transaction context in this basic mock test,
        // TransactionSynchronizationManager.isSynchronizationActive() will be false,
        // and task.run() will execute immediately in triggerAutoBidProcessing.

        var response = biddingService.placeBid("10", "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL);

        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("120.00");
        verify(bidExecutor).execute("10", "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL);
        verify(autoBidService).processAutoBidsForAuction("10", "2");
    }

    @Test
    void placeBidRejectsUserNotFound() {
        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> biddingService.placeBid("10", "bidder@test.com", new BigDecimal("120.00"), BidType.MANUAL))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Bidder not found");

        verifyNoInteractions(bidExecutor);
        verifyNoInteractions(autoBidService);
    }

    @Test
    void placeBidAutoBidDoesNotTriggerProcessing() {
        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.of(bidder));
        when(bidExecutor.execute("10", "bidder@test.com", new BigDecimal("120.00"), BidType.AUTO))
            .thenReturn(mockResponse);

        var response = biddingService.placeBid("10", "bidder@test.com", new BigDecimal("120.00"), BidType.AUTO);

        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        verify(bidExecutor).execute("10", "bidder@test.com", new BigDecimal("120.00"), BidType.AUTO);
        verifyNoInteractions(autoBidService);
    }
}

