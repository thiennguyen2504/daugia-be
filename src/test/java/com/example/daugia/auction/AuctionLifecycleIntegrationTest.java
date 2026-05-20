package com.example.daugia.auction;

import com.example.daugia.TestUtils;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.auction.scheduler.AuctionScheduler;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
@org.springframework.test.context.TestPropertySource(properties = "auction.scheduler.enabled=true")
public class AuctionLifecycleIntegrationTest {

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

    @Autowired
    private AuctionScheduler auctionScheduler;

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private net.javacrumbs.shedlock.core.LockProvider lockProvider;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(lockProvider.lock(any())).thenReturn(Optional.of(() -> {}));
    }

    @Test
    void sellerCreatesAuction_StatusPending() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller@example.com", "0111111111", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat");
        String token = TestUtils.generateJwt(seller.getEmail(), "SELLER", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("productName", "New Phone");
        request.put("description", "A great phone");
        request.put("startingPrice", 100);
        request.put("bidIncrement", 10);
        request.put("buyNowPrice", 1000);
        request.put("categoryId", category.getId());
        request.put("biddingStartTime", LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        request.put("biddingEndTime", LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "request",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/auctions")
            .file(requestPart)
                .header("Authorization", "Bearer " + token)
            .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        Auction created = auctionRepository.findAll().get(0);
        assertThat(created.getStatus()).isEqualTo(AuctionStatus.PENDING);
    }

    @Test
    void adminApprovesAuction_StatusApproved() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller2@example.com", "0111111112", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat2");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.PENDING, LocalDateTime.now().plusDays(1));

        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("approved", true);

        mockMvc.perform(put("/api/v1/admin/auctions/" + auction.getId() + "/review")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Auction updated = auctionRepository.findById(auction.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.APPROVED);
    }

    @Test
    void adminRejectsAuctionWithoutReason_Returns400() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller3@example.com", "0111111113", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat3");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.PENDING, LocalDateTime.now().plusDays(1));

        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("approved", false);
        // No reason provided

        mockMvc.perform(put("/api/v1/admin/auctions/" + auction.getId() + "/review")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminRejectsAuctionWithReason_StatusRejected() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller4@example.com", "0111111114", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat4");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.PENDING, LocalDateTime.now().plusDays(1));

        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("approved", false);
        request.put("rejectionReason", "Incomplete description");

        mockMvc.perform(put("/api/v1/admin/auctions/" + auction.getId() + "/review")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Auction updated = auctionRepository.findById(auction.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.REJECTED);
        assertThat(updated.getRejectionReason()).isEqualTo("Incomplete description");
    }

    @Test
    void activateApprovedAuctions_Scheduler_Activates() {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller5@example.com", "0111111115", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat5");
        
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.APPROVED, LocalDateTime.now().plusDays(1));
        auction.setBiddingStartTime(LocalDateTime.now().minusMinutes(5)); // Past start time
        auctionRepository.save(auction);

        auctionScheduler.activateApprovedAuctions();

        Auction updated = auctionRepository.findById(auction.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    void endActiveAuctions_Scheduler_Ends() {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller6@example.com", "0111111116", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat6");
        
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ACTIVE, LocalDateTime.now().minusMinutes(5));
        auctionRepository.save(auction);

        auctionScheduler.endActiveAuctions();

        Auction updated = auctionRepository.findById(auction.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }

    @Test
    void unauthenticatedUserViewsPendingAuction_Returns404() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller7@example.com", "0111111117", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat7");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.PENDING, LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/v1/auctions/" + auction.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sellerViewsOwnPendingAuction_Allowed() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller8@example.com", "0111111118", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat8");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.PENDING, LocalDateTime.now().plusDays(1));

        String token = TestUtils.generateJwt(seller.getEmail(), "SELLER", jwtSecret);

        mockMvc.perform(get("/api/v1/auctions/" + auction.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
