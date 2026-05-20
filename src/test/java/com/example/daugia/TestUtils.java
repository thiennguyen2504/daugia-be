package com.example.daugia;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.category.entity.Category;
import com.example.daugia.user.entity.Role;
import com.example.daugia.user.entity.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class TestUtils {

    public static Role createRole(CrudRepository<Role, String> roleRepo, String roleName) {
        Role role = Role.builder().name(roleName).build();
        return roleRepo.save(role);
    }

    public static User createUser(CrudRepository<User, String> userRepo, PasswordEncoder passwordEncoder, String email, String phone, Role role) {
        User user = User.builder()
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode("Password123!"))
                .firstname("Test")
                .lastname("User")
                .role(role)
                .enabled(true)
                .locked(false)
                .build();
        return userRepo.save(user);
    }

    public static Category createCategory(CrudRepository<Category, String> categoryRepo, String name) {
        Category category = Category.builder()
                .name(name)
                .description("Test Category Description")
                .deleted(false)
                .build();
        return categoryRepo.save(category);
    }

    public static Auction createAuction(CrudRepository<Auction, String> auctionRepo, User seller, Category category, AuctionStatus status, LocalDateTime endTime) {
        Auction auction = Auction.builder()
                .productName("Test Auction " + UUID.randomUUID())
                .description("Test Description")
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .buyNowPrice(new BigDecimal("1000.00"))
                .status(status)
                .seller(seller)
                .category(category)
                .biddingStartTime(LocalDateTime.now().minusDays(1))
                .biddingEndTime(endTime)
                .endTime(endTime)
                .extensionCount(0)
                .build();
        return auctionRepo.save(auction);
    }

    public static String generateJwt(String email, String role, String secret) {
        try {
            MACSigner signer = new MACSigner(secret.getBytes());
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .claim("role", role)
                    .issueTime(new Date())
                    .expirationTime(new Date(new Date().getTime() + 3600000))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }
}
