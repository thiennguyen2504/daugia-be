package com.example.daugia.payment;

import com.example.daugia.TestUtils;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.payment.entity.Payment;
import com.example.daugia.payment.entity.PaymentStatus;
import com.example.daugia.payment.repository.PaymentRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
public class PaymentIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret-key}")
    private String jwtSecret;
    
    @Value("${vnpay.hash-secret}")
    private String vnpayHashSecret;

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

    private String hmacSHA512(String secret, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(key);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
    
    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    @Test
    void createPayment_Winner_Success() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller@example.com", "0111111111", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User winner = TestUtils.createUser(userRepository, passwordEncoder, "winner@example.com", "0111111112", bidderRole);
        
        auction.setCurrentWinner(winner);
        auctionRepository.save(auction);

        String token = TestUtils.generateJwt(winner.getEmail(), "BIDDER", jwtSecret);

        mockMvc.perform(post("/api/v1/payments/auction/" + auction.getId() + "/create")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void createPayment_NonWinner_Returns403() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller2@example.com", "0111111113", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat2");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User winner = TestUtils.createUser(userRepository, passwordEncoder, "winner2@example.com", "0111111114", bidderRole);
        User nonWinner = TestUtils.createUser(userRepository, passwordEncoder, "loser@example.com", "0111111115", bidderRole);
        
        auction.setCurrentWinner(winner);
        auctionRepository.save(auction);

        String token = TestUtils.generateJwt(nonWinner.getEmail(), "BIDDER", jwtSecret);

        mockMvc.perform(post("/api/v1/payments/auction/" + auction.getId() + "/create")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void vnpayReturn_ValidHmac_PaymentPaid() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller3@example.com", "0111111116", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat3");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User winner = TestUtils.createUser(userRepository, passwordEncoder, "winner3@example.com", "0111111117", bidderRole);
        
        auction.setCurrentWinner(winner);
        auctionRepository.save(auction);

        Payment payment = Payment.builder()
                .auction(auction)
                .payer(winner)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .vnpayTxnRef("TXN12345")
                .build();
        paymentRepository.save(payment);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNP123");
        params.put("vnp_TxnRef", "TXN12345");
        params.put("vnp_OrderInfo", "Payment Test");
        
        String hashData = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    try {
                        return encode(e.getKey()) + "=" + encode(e.getValue());
                    } catch (Exception ex) {
                        return "";
                    }
                })
                .collect(Collectors.joining("&"));
                
        String secureHash = hmacSHA512(vnpayHashSecret, hashData);

        mockMvc.perform(get("/api/v1/payments/vnpay-return")
                .param("vnp_Amount", params.get("vnp_Amount"))
                .param("vnp_ResponseCode", params.get("vnp_ResponseCode"))
                .param("vnp_TransactionNo", params.get("vnp_TransactionNo"))
                .param("vnp_TxnRef", params.get("vnp_TxnRef"))
                .param("vnp_OrderInfo", params.get("vnp_OrderInfo"))
                .param("vnp_SecureHash", secureHash))
                .andExpect(status().isOk());

        Payment updated = paymentRepository.findById(payment.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void vnpayReturn_InvalidHmac_PaymentFailed() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller4@example.com", "0111111118", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat4");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User winner = TestUtils.createUser(userRepository, passwordEncoder, "winner4@example.com", "0111111119", bidderRole);
        
        auction.setCurrentWinner(winner);
        auctionRepository.save(auction);

        Payment payment = Payment.builder()
                .auction(auction)
                .payer(winner)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .vnpayTxnRef("TXN999")
                .build();
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/vnpay-return")
                .param("vnp_Amount", "10000")
                .param("vnp_ResponseCode", "00")
                .param("vnp_TxnRef", "TXN999")
                .param("vnp_SecureHash", "invalidhash123"))
                .andExpect(status().isOk());

        Payment updated = paymentRepository.findById(payment.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
    
    @Test
    void getPaymentDetails_Winner_Success() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller5@example.com", "0111111120", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat5");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User winner = TestUtils.createUser(userRepository, passwordEncoder, "winner5@example.com", "0111111121", bidderRole);
        
        auction.setCurrentWinner(winner);
        auctionRepository.save(auction);

        Payment payment = Payment.builder()
                .auction(auction)
                .payer(winner)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .vnpayTxnRef("TXN100")
                .build();
        paymentRepository.save(payment);

        String token = TestUtils.generateJwt(winner.getEmail(), "BIDDER", jwtSecret);

        mockMvc.perform(get("/api/v1/payments/auction/" + auction.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getMyPayments_Bidder_ReturnsWonAuctions() throws Exception {
        Role sellerRole = TestUtils.createRole(roleRepository, "SELLER");
        User seller = TestUtils.createUser(userRepository, passwordEncoder, "seller6@example.com", "0111111122", sellerRole);
        Category category = TestUtils.createCategory(categoryRepository, "Cat6");
        Auction auction = TestUtils.createAuction(auctionRepository, seller, category, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

        Role bidderRole = TestUtils.createRole(roleRepository, "BIDDER");
        User winner = TestUtils.createUser(userRepository, passwordEncoder, "winner6@example.com", "0111111123", bidderRole);
        
        auction.setCurrentWinner(winner);
        auctionRepository.save(auction);

        String token = TestUtils.generateJwt(winner.getEmail(), "BIDDER", jwtSecret);

        mockMvc.perform(get("/api/v1/payments/my")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
