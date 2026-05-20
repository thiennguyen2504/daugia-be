package com.example.daugia.bidding;

import com.example.daugia.TestUtils;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.RoleRepository;
import com.example.daugia.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
public class BidRestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private ZSetOperations<String, String> zSetOperations;

    @MockBean
    private ListOperations<String, String> listOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void placeBid_Success_ReturnsAccepted() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller@example.com", "0111111111", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder@example.com", "0111111112", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", 120);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void placeBid_TooLow_ReturnsRejected() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller2@example.com", "0111111113", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat2");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        // Current price is 100, bid increment is 10, minimum bid is 110.

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder2@example.com", "0111111114", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", 105);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void sellerBidsOwnAuction_ReturnsRejected() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller3@example.com", "0111111115", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat3");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        String token = TestUtils.generateJwt(seller.getEmail(), "SELLER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", 120);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getBids_ReturnsBidHistory() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller4@example.com", "0111111116", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat4");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/v1/auctions/" + auction.getId() + "/bids"))
                .andExpect(status().isOk());
    }

    @Test
    void getBidHistory_ReturnsImmutableHistory() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller5@example.com", "0111111117", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat5");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        String token = TestUtils.generateJwt(seller.getEmail(), "SELLER", jwtSecret);

        mockMvc.perform(get("/api/v1/auctions/" + auction.getId() + "/history")
            .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getLeaderboard_ReturnsLeaderboard() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller6@example.com", "0111111118", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat6");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/v1/auctions/" + auction.getId() + "/leaderboard"))
                .andExpect(status().isOk());
    }

    @Test
    void antiSnipe_BidNearEnd_ExtendsEndTime() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller7@example.com", "0111111119", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat7");
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(30); // within 60s
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, endTime);

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder7@example.com", "0111111120", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", 120);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Auction updated = auctionRepository.findById(auction.getId()).get();
        assertThat(updated.getEndTime()).isAfter(endTime); // Should be extended by 120s
        assertThat(updated.getExtensionCount()).isEqualTo(1);
    }

    @Test
    void buyNow_AuctionEndsImmediately() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller8@example.com", "0111111121", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat8");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder8@example.com", "0111111122", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000); // Equal to buyNowPrice

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Auction updated = auctionRepository.findById(auction.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }
}
