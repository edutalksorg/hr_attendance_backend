package com.megamart.backend.config;

import com.megamart.backend.attendance.AttendanceRepository;
import com.megamart.backend.documents.DocumentRepository;
import com.megamart.backend.helpdesk.SupportTicketRepository;
import com.megamart.backend.notes.NoteRepository;
import com.megamart.backend.notification.NotificationRepository;
import com.megamart.backend.performance.PerformanceGoalRepository;
import com.megamart.backend.performance.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DataCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanupScheduler.class);

    private final AttendanceRepository attendanceRepository;
    private final NotificationRepository notificationRepository;
    private final NoteRepository noteRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final DocumentRepository documentRepository;
    private final com.megamart.backend.documents.DocumentService documentService;
    private final PerformanceGoalRepository performanceGoalRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final com.megamart.backend.leave.LeaveRequestRepository leaveRequestRepository;

    /**
     * Run every day at 1:00 AM.
     * Deletes records older than 60 days (retention policy).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldData() {
        logger.info("Starting master data cleanup (retention: 60 days)...");
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(60);

        try {
            attendanceRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old attendance records.");

            notificationRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old notifications.");

            noteRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old notes.");

            supportTicketRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old support tickets.");

            // For documents, we use the service to ensure file deletion
            java.util.List<com.megamart.backend.documents.Document> oldDocs = documentRepository
                    .findByCreatedAtBefore(threshold);
            for (com.megamart.backend.documents.Document doc : oldDocs) {
                documentService.delete(doc.getId());
            }
            logger.info("Cleaned up {} old documents and their files.", oldDocs.size());

            performanceGoalRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old performance goals.");

            performanceReviewRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old performance reviews.");

            leaveRequestRepository.deleteByCreatedAtBefore(threshold);
            logger.info("Cleaned up old leave requests.");

            logger.info("Master data cleanup completed successfully.");
        } catch (Exception e) {
            logger.error("Error during data cleanup: {}", e.getMessage(), e);
        }
    }
}
