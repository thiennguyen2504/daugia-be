package com.example.daugia.deposit;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.common.exception.DuplicateResourceException;
import com.example.daugia.deposit.entity.DepositStatus;
import com.example.daugia.deposit.service.DepositService;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.RoleRepository;
import com.example.daugia.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "auction.redis.pubsub.enabled=false"
})
@Transactional
class DepositServiceIntegrationTest {

    @jakarta.annotation.Resource DepositService depositService;
    @jakarta.annotation.Resource AuctionRepository auctionRepository;
    @jakarta.annotation.Resource UserRepository userRepository;
    @jakarta.annotation.Resource RoleRepository roleRepository;
    @jakarta.annotation.Resource CategoryRepository categoryRepository;

    @Test
    void holdReleaseAndDuplicateHold() {
        User seller = user("seller-deposit@test.com", "SELLER");
        User bidder = user("bidder-deposit@test.com", "BIDDER");
        Auction auction = auction(seller);

        var deposit = depositService.holdDeposit(auction.getId(), bidder.getId(), new BigDecimal("10.00"));

        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.HELD);
        assertThat(depositService.hasDeposit(auction.getId(), bidder.getId())).isTrue();
        assertThatThrownBy(() -> depositService.holdDeposit(auction.getId(), bidder.getId(), new BigDecimal("10.00")))
                .isInstanceOf(DuplicateResourceException.class);

        depositService.releaseDeposit(auction.getId(), bidder.getId());
        assertThat(depositService.hasDeposit(auction.getId(), bidder.getId())).isFalse();
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
                .name("Deposit Test " + System.nanoTime())
                .description("test")
                .deleted(false)
                .createdBy("test")
                .build());
        return auctionRepository.save(Auction.builder()
                .productName("Deposit Auction")
                .description("test")
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .biddingStartTime(LocalDateTime.now().minusHours(1))
                .biddingEndTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .category(category)
                .build());
    }
}
