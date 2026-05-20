package com.example.daugia.category;

import com.example.daugia.TestUtils;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
public class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getCategories_Success() throws Exception {
        TestUtils.createCategory(categoryRepository, "Cat 1");
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void createCategory_Admin_Success() throws Exception {
        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);
        
        Map<String, String> request = new HashMap<>();
        request.put("name", "New Category");
        request.put("description", "Desc");

        mockMvc.perform(post("/api/v1/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(categoryRepository.findAll().stream().anyMatch(c -> c.getName().equals("New Category"))).isTrue();
    }

    @Test
    void createCategory_NonAdmin_Returns403() throws Exception {
        String bidderToken = TestUtils.generateJwt("bidder@example.com", "BIDDER", jwtSecret);
        
        Map<String, String> request = new HashMap<>();
        request.put("name", "New Category");

        mockMvc.perform(post("/api/v1/categories")
                .header("Authorization", "Bearer " + bidderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCategory_Admin_Success() throws Exception {
        Category cat = TestUtils.createCategory(categoryRepository, "Old Cat");
        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        Map<String, String> request = new HashMap<>();
        request.put("name", "Updated Cat");

        mockMvc.perform(put("/api/v1/categories/" + cat.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Category updated = categoryRepository.findById(cat.getId()).get();
        assertThat(updated.getName()).isEqualTo("Updated Cat");
    }

    @Test
    void deleteCategory_Admin_Success() throws Exception {
        Category cat = TestUtils.createCategory(categoryRepository, "To Delete");
        String adminToken = TestUtils.generateJwt("admin@example.com", "ADMIN", jwtSecret);

        mockMvc.perform(delete("/api/v1/categories/" + cat.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Category deleted = categoryRepository.findById(cat.getId()).get();
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    void getCategoryById_Success() throws Exception {
        Category cat = TestUtils.createCategory(categoryRepository, "Get Me");
        
        mockMvc.perform(get("/api/v1/categories/" + cat.getId()))
                .andExpect(status().isOk());
    }
}
