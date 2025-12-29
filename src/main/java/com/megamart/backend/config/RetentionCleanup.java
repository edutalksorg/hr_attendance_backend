package com.megamart.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetentionCleanup {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetentionCleanup.class);
    private final JdbcTemplate jdbc;

    /**
     * Attendance and activity data must be automatically deleted on the 61st day
     * from the date of entry (i.e., keep exactly 60 days of history).
     * Runs daily at 00:01 AM.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void purgeOld() {
        log.info("Starting automated data retention cleanup (60 days retention policy)...");
        try {
            int loginPurged = jdbc.update("DELETE FROM login_history WHERE created_at < now() - INTERVAL '60 days'");
            int navPurged = jdbc.update("DELETE FROM navigation_history WHERE created_at < now() - INTERVAL '60 days'");
            int marketPurged = jdbc
                    .update("DELETE FROM marketing_history WHERE created_at < now() - INTERVAL '60 days'");

            // Documents are also purged if older than 60 days (e.g. temporary logs/exports)
            int docsPurged = jdbc.update("DELETE FROM documents WHERE created_at < now() - INTERVAL '60 days'");

            log.info(
                    "Cleanup completed. Purged: {} login records, {} navigation records, {} marketing records, {} document records.",
                    loginPurged, navPurged, marketPurged, docsPurged);
        } catch (Exception e) {
            log.error("Failed to perform scheduled data cleanup: {}", e.getMessage());
        }
    }
}
