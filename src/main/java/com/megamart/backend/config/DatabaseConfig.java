package com.megamart.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DatabaseConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("🛠️ Attempting to patch database schema...");
            // Patch for photo_url column type
            jdbcTemplate.execute("ALTER TABLE user_profiles ALTER COLUMN photo_url TYPE TEXT");
            logger.info("✅ Successfully altered photo_url column to TEXT");
        } catch (Exception e) {
            logger.warn("⚠️ Could not alter photo_url column (it might already be correct or table missing): {}",
                    e.getMessage());
        }
    }
}
