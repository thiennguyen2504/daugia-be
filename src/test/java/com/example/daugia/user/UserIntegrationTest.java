package com.example.daugia.user;

import com.example.daugia.TestUtils;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getMe_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "me@example.com", "0111111111", role);
        String token = TestUtils.generateJwt(user.getEmail(), "BIDDER", jwtSecret);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfile_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "me2@example.com", "0111111112", role);
        String token = TestUtils.generateJwt(user.getEmail(), "BIDDER", jwtSecret);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/users/profile")
                .param("fullName", "UpdatedFirst UpdatedLast")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).get();
        assertThat(updated.getFirstname()).isEqualTo("UpdatedFirst");
        assertThat(updated.getLastname()).isEqualTo("UpdatedLast");
    }

    @Test
    void getAllUsers_Admin_Success() throws Exception {
        String token = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void lockAndUnlockUser_Admin_Success() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "lock@example.com", "0111111113", role);
        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        Map<String, Object> request = new HashMap<>();
        request.put("reason", "Routine account review");

        // Lock
        mockMvc.perform(put("/api/v1/admin/users/" + user.getId() + "/lock")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User lockedUser = userRepository.findById(user.getId()).get();
        assertThat(lockedUser.isLocked()).isTrue();

        // Unlock
        mockMvc.perform(put("/api/v1/admin/users/" + user.getId() + "/unlock")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User unlockedUser = userRepository.findById(user.getId()).get();
        assertThat(unlockedUser.isLocked()).isFalse();
    }

    @Test
    void authenticate_LockedUser_Returns403() throws Exception {
        Role role = TestUtils.createRole(roleRepository, "BIDDER");
        User user = TestUtils.createUser(userRepository, passwordEncoder, "lock2@example.com", "0111111114", role);
        user.setLocked(true);
        userRepository.save(user);

        Map<String, Object> request = new HashMap<>();
        request.put("identifier", "lock2@example.com");
        request.put("password", "Password123!");

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
