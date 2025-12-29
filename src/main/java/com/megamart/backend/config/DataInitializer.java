package com.megamart.backend.config;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.user.UserRole;
import com.megamart.backend.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed admin and HR users if they don't exist

        if (userRepository.findByEmail("megamart.dvst@gmail.com").isEmpty()) {
            User admin = User.builder()
                    .fullName("Admin")
                    .email("megamart.dvst@gmail.com")
                    .password(passwordEncoder.encode("edutalks@321"))
                    .phone("1234567890")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();
            userRepository.save(admin);
            System.out.println("Admin user created");
        }

        if (userRepository.findByEmail("hr@megamart.com").isEmpty()) {
            User hr = User.builder()
                    .fullName("HR Manager")
                    .email("hr@megamart.com")
                    .password(passwordEncoder.encode("Hr123@"))
                    .phone("9876543210")
                    .role(UserRole.HR)
                    .status(UserStatus.ACTIVE)
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();
            userRepository.save(hr);
            System.out.println("HR user created");
        }
    }
}
