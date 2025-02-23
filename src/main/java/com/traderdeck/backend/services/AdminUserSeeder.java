package com.traderdeck.backend.services;

import com.traderdeck.backend.models.User;
import com.traderdeck.backend.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AdminUserSeeder {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminUserSeeder(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seedUsers() {
        Optional<User> existingUser = userRepository.findByEmail("yacine.boulaioune@gmail.com");

        if (existingUser.isEmpty()) {
            String hashedPassword = passwordEncoder.encode("SecureAdmin123");

            User user = User.builder()
                    .username("yacbln")
                    .email("yacine.boulaioune@gmail.com")
                    .password(hashedPassword)
                    .role("ADMIN")
                    .build();

            userRepository.save(user);
            System.out.println("✅ Seeded default admin user: yacbln");
        } else {
            System.out.println("⚠️ Admin user already exists, skipping seeding.");
        }
    }
}
