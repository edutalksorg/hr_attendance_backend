package com.megamart.backend.attendance;

import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Component // Disabled in favor of GeneralDataCleanupScheduler
@RequiredArgsConstructor
public class AttendanceCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceCleanupScheduler.class);
    private final AttendanceRepository attendanceRepository;

    // Run every day at midnight (00:00:00)
    // @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupOldAttendance() {
        logger.info("Starting scheduled cleanup of old attendance records (older than 60 days)...");
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(60);
        attendanceRepository.deleteByCreatedAtBefore(threshold);
        logger.info("Cleanup completed.");
    }
}
