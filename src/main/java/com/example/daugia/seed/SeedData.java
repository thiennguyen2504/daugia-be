package com.example.daugia.seed;

import com.example.daugia.entity.Role;
import com.example.daugia.entity.User;
import com.example.daugia.repository.RoleRepository;
import com.example.daugia.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedData implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        }

        private void seedAccount(String email, String phone, String firstname, String lastname, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        userRepository.save(User.builder()
            .firstname(firstname)
            .lastname(lastname)
            .email(email)
            .phone(phone)
            .password(passwordEncoder.encode(rawPassword))
            .enabled(true)
            .roles(roles)
            .build());

        log.info("Seeded account {}", email);
    }
}
