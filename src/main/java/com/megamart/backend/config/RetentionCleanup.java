package com.megamart.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetentionCleanup {
    private final JdbcTemplate jdbc;

    // once per day at 03:00
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOld() {
        jdbc.update("DELETE FROM login_history WHERE created_at < now() - INTERVAL '60 days'");
        jdbc.update("DELETE FROM navigation_history WHERE created_at < now() - INTERVAL '60 days'");
        jdbc.update("DELETE FROM marketing_history WHERE created_at < now() - INTERVAL '60 days'");
        jdbc.update("DELETE FROM documents WHERE created_at < now() - INTERVAL '60 days'");
    }
}
