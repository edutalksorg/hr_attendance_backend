package com.megamart.backend.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanupScheduler.class);

    /**
     * Deletes records older than 60 days (retention policy).
     * DISABLED to ensure data persistence.
     */
    // @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldData() {
        // Auto-deletion disabled to ensure data persistence.
        // Records should only be deleted manually by authorized roles.
        logger.info("Master data cleanup is disabled (retention policy removed).");
    }
}
