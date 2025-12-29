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

            // Patch for role constraints that might block new enum values like MANAGER
            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
                logger.info("✅ Dropped users_role_check if it existed");
            } catch (Exception e) {
                logger.warn("Could not drop users_role_check: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE approvals DROP CONSTRAINT IF EXISTS approvals_role_after_check");
                logger.info("✅ Dropped approvals_role_after_check if it existed");
            } catch (Exception e) {
                logger.warn("Could not drop approvals_role_after_check: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Database patching failed: {}", e.getMessage());
        }
    }
}
