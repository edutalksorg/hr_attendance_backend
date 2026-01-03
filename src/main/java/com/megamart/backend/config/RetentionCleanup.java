package com.megamart.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetentionCleanup {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetentionCleanup.class);

    /**
     * Attendance and activity data must be automatically deleted on the 61st day
     * from the date of entry (i.e., keep exactly 60 days of history).
     * DISABLED to ensure data persistence.
     */
    // @Scheduled(cron = "0 1 0 * * *")
    public void purgeOld() {
        // Auto-deletion disabled to ensure data persistence.
        // Records should only be deleted manually by authorized roles.
        log.info("Automated data retention cleanup is disabled (retention policy removed).");
    }
}
