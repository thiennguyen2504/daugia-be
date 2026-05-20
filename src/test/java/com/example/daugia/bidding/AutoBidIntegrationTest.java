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
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
public class AutoBidIntegrationTest {

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

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void configureAutoBid_Success() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller@example.com", "0111111111", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder@example.com", "0111111112", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("maxAmount", 500);
        request.put("increment", 10);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/auto-bid")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getAutoBid_ReturnsConfig() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller2@example.com", "0111111113", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat2");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder2@example.com", "0111111114", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("maxAmount", 500);
        request.put("increment", 10);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/auto-bid")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auctions/" + auction.getId() + "/auto-bid")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAutoBid_DeactivatesConfig() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller3@example.com", "0111111115", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat3");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User bidder = TestUtils.createUser(userRepository, passwordEncoder, "bidder3@example.com", "0111111116", bidderRole);
        String token = TestUtils.generateJwt(bidder.getEmail(), "BIDDER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("maxAmount", 500);

        mockMvc.perform(post("/api/v1/auctions/" + auction.getId() + "/auto-bid")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auctions/" + auction.getId() + "/auto-bid")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
