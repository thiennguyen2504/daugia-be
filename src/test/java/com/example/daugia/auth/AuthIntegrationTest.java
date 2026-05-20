package com.example.daugia.auth;

import com.example.daugia.TestUtils;
import com.example.daugia.user.entity.Otp;
import com.example.daugia.user.entity.OtpPurpose;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.OtpRepository;
import com.example.daugia.user.repository.RoleRepository;
import com.example.daugia.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // OTP mock
        when(valueOperations.get("OTP:verify@example.com")).thenReturn("123456");
        when(valueOperations.get("OTP:forgot@example.com")).thenReturn("123456");
    }

    @Test
    void register_Success() throws Exception {
        TestUtils.createRole(roleRepository, "BIDDER");
        Map<String, Object> request = new HashMap<>();
        request.put("fullName", "John Doe");
        request.put("phone", "0123456789");
        request.put("email", "newuser@example.com");
        request.put("password", "Password123!");
        request.put("confirmPassword", "Password123!");
        request.put("role", "BIDDER");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail("newuser@example.com")).isPresent();
    }

    @Test
    void verifyOtp_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "verify@example.com", "0123456788", role);
        user.setEnabled(false);
        userRepository.save(user);

        otpRepository.save(Otp.builder()
            .email("verify@example.com")
            .code("123456")
            .purpose(OtpPurpose.REGISTRATION)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .used(false)
            .build());

        Map<String, Object> request = new HashMap<>();
        request.put("email", "verify@example.com");
        request.put("otp", "123456");
        request.put("purpose", "REGISTRATION");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").exists());

        User verifiedUser = userRepository.findByEmail("verify@example.com").get();
        assertThat(verifiedUser.isEnabled()).isTrue();
    }

    @Test
    void authenticate_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        TestUtils.createUser(userRepository, passwordEncoder, "auth@example.com", "0123456787", role);

        Map<String, Object> request = new HashMap<>();
        request.put("identifier", "auth@example.com");
        request.put("password", "Password123!");

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").exists());
    }

    @Test
    void authenticate_WrongPassword_Returns401() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        TestUtils.createUser(userRepository, passwordEncoder, "auth@example.com", "0123456787", role);

        Map<String, Object> request = new HashMap<>();
        request.put("identifier", "auth@example.com");
        request.put("password", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_Unverified_Returns403() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "auth@example.com", "0123456787", role);
        user.setEnabled(false);
        userRepository.save(user);

        Map<String, Object> request = new HashMap<>();
        request.put("identifier", "auth@example.com");
        request.put("password", "Password123!");

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgotPassword_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        TestUtils.createUser(userRepository, passwordEncoder, "forgot@example.com", "0123456786", role);

        Map<String, Object> request = new HashMap<>();
        request.put("email", "forgot@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "forgot@example.com", "0123456786", role);

        otpRepository.save(Otp.builder()
            .email("forgot@example.com")
            .code("123456")
            .purpose(OtpPurpose.FORGOT_PASSWORD)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .used(false)
            .build());

        Map<String, Object> request = new HashMap<>();
        request.put("email", "forgot@example.com");
        request.put("otp", "123456");
        request.put("newPassword", "NewPassword123!");
        request.put("confirmPassword", "NewPassword123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                
        User updatedUser = userRepository.findByEmail("forgot@example.com").get();
        assertThat(passwordEncoder.matches("NewPassword123!", updatedUser.getPassword())).isTrue();
    }
}
