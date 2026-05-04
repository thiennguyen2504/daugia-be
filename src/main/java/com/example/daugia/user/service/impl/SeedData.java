package com.example.daugia.user.service.impl;

import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.RoleRepository;
import com.example.daugia.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedData implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        Role sellerRole = roleRepository.findByName("SELLER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("SELLER").build()));

        Role bidderRole = roleRepository.findByName("BIDDER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("BIDDER").build()));

        seedAccount("admin@gmail.com", "0900000001", "Admin", "System", "Admin@123", adminRole);
        seedAccount("seller@gmail.com", "0900000002", "Nguyen", "Van Seller", "Seller@123", sellerRole);
        seedAccount("bidder@gmail.com", "0900000003", "Tran", "Thi Bidder", "Bidder@123", bidderRole);

        log.info("Seed data initialized with roles: ADMIN, SELLER, BIDDER");

        seedCategory("Electronics", "Electronic devices and accessories");
        seedCategory("Real Estate", "Land, apartments and commercial property");
        seedCategory("Vehicles", "Cars, motorbikes and other vehicles");
        seedCategory("Luxury Goods", "Watches, jewelry and premium items");
        seedCategory("Art & Collectibles", "Paintings, sculptures and rare collectibles");
    }

    private void seedAccount(String email, String phone, String firstname, String lastname, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        userRepository.save(User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .role(role)
                .build());

        log.info("Seeded account {}", email);
    }

    private void seedCategory(String name, String description) {
        if (!categoryRepository.existsByNameIgnoreCase(name)) {
            categoryRepository.save(Category.builder()
                    .name(name)
                    .description(description)
                    .deleted(false)
                    .createdBy("admin@gmail.com")
                    .build());
            log.info("Seeded category: {}", name);
        }
    }
}
