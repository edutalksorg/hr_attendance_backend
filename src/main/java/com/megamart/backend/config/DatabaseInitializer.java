package com.megamart.backend.config;

import com.megamart.backend.user.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user if not exists
        if (!userRepository.existsByEmail("megamart.dvst@gmail.com")) {
            User admin = User.builder()
                    .fullName("Admin")
                    .email("megamart.dvst@gmail.com")
                    .password(passwordEncoder.encode("edutalks@321"))
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            userRepository.save(admin);
            logger.info("✅ Created ADMIN user: {}", admin.getEmail());
        } else {
            logger.info("ℹ️  ADMIN user already exists");
        }

        // Create HR user if not exists
        if (!userRepository.existsByEmail("Prasanna122hr@gmail.com")) {
            User hr = User.builder()
                    .fullName("Prasanna HR")
                    .email("Prasanna122hr@gmail.com")
                    .password(passwordEncoder.encode("Hr123@"))
                    .role(UserRole.HR)
                    .status(UserStatus.ACTIVE)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            userRepository.save(hr);
            logger.info("✅ Created HR user: {}", hr.getEmail());
        } else {
            logger.info("ℹ️  HR user already exists");
        }

        // Create test employee if not exists
        if (!userRepository.existsByEmail("employee@test.com")) {
            User employee = User.builder()
                    .fullName("Test Employee")
                    .email("employee@test.com")
                    .password(passwordEncoder.encode("Test123@"))
                    .role(UserRole.EMPLOYEE)
                    .status(UserStatus.ACTIVE)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            userRepository.save(employee);
            logger.info("✅ Created EMPLOYEE user: {}", employee.getEmail());
        } else {
            logger.info("ℹ️  EMPLOYEE user already exists");
        }

        logger.info("🎉 Database initialization complete!");
    }
}
