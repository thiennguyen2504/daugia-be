package com.example.daugia.bidding;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.repository.BidRepository;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.bidding.service.BiddingService;
import com.example.daugia.bidding.service.LeaderboardService;
import com.example.daugia.bidding.service.RedisBidPublisher;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.deposit.service.DepositService;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.RoleRepository;
import com.example.daugia.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "auction.redis.pubsub.enabled=false",
        "auction.antinsiping.window-seconds=60",
        "auction.antinsiping.extension-seconds=120"
})
class BiddingFlowIntegrationTest {

    @jakarta.annotation.Resource BiddingService biddingService;
    @jakarta.annotation.Resource AutoBidService autoBidService;
    @jakarta.annotation.Resource DepositService depositService;
    @jakarta.annotation.Resource BidRepository bidRepository;
    @jakarta.annotation.Resource AuctionRepository auctionRepository;
    @jakarta.annotation.Resource UserRepository userRepository;
    @jakarta.annotation.Resource RoleRepository roleRepository;
    @jakarta.annotation.Resource CategoryRepository categoryRepository;
    @MockBean RedisBidPublisher redisBidPublisher;
    @MockBean LeaderboardService leaderboardService;

    @Test
    void depositBidAutoBidAndAntiSnipeExtension() throws InterruptedException {
        User seller = user("seller-flow@test.com", "SELLER");
        User manualBidder = user("manual-flow@test.com", "BIDDER");
        User autoBidder = user("auto-flow@test.com", "BIDDER");
        Auction auction = auction(seller);

        depositService.holdDeposit(auction.getId(), manualBidder.getId(), new BigDecimal("10.00"));
        depositService.holdDeposit(auction.getId(), autoBidder.getId(), new BigDecimal("10.00"));
        autoBidService.upsertConfig(auction.getId(), autoBidder.getEmail(), new BigDecimal("150.00"));

        LocalDateTime originalEndTime = auction.getEndTime();
        var manualResponse = biddingService.placeBid(auction.getId(), manualBidder.getEmail(), new BigDecimal("120.00"), BidType.MANUAL);

        assertThat(manualResponse.getStatus()).isEqualTo("ACCEPTED");
        assertThat(manualResponse.getEndTime()).isAfter(originalEndTime);

        for (int i = 0; i < 80 && bidRepository.count() < 2; i++) {
            Thread.sleep(100);
        }

        Auction reloaded = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(bidRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(reloaded.getCurrentPrice()).isGreaterThanOrEqualTo(new BigDecimal("130.00"));
        assertThat(reloaded.getEndTime()).isAfter(originalEndTime);
    }

    private User user(String email, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
        return userRepository.save(User.builder()
                .email(email)
                .phone(email.hashCode() + "")
                .firstname(roleName)
                .lastname("User")
                .password("password")
                .enabled(true)
                .role(role)
                .build());
    }

    private Auction auction(User seller) {
        Category category = categoryRepository.save(Category.builder()
                .name("Flow Test " + System.nanoTime())
                .description("test")
                .deleted(false)
                .createdBy("test")
                .build());
        return auctionRepository.save(Auction.builder()
                .productName("Flow Auction")
                .description("test")
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .biddingStartTime(LocalDateTime.now().minusHours(1))
                .biddingEndTime(LocalDateTime.now().plusSeconds(30))
                .endTime(LocalDateTime.now().plusSeconds(30))
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .category(category)
                .build());
    }
}
