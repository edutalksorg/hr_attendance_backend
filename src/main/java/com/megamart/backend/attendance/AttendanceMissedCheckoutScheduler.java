package com.megamart.backend.attendance;

import com.megamart.backend.email.EmailService;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.shift.Shift;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class AttendanceMissedCheckoutScheduler {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    @Transactional
    public void checkMissedCheckouts() {
        List<Attendance> activeSessions = attendanceRepository.findByLogoutTimeIsNull();
        OffsetDateTime now = OffsetDateTime.now();

        for (Attendance att : activeSessions) {
            try {
                processAttendanceSession(att, now);
            } catch (Exception e) {
                // Log and continue to next session
                System.err.println(
                        "Error processing missed checkout for attendance " + att.getId() + ": " + e.getMessage());
            }
        }
    }

    private void processAttendanceSession(Attendance att, OffsetDateTime now) throws Exception {
        Map<String, Object> meta = getMetadata(att);
        String currentStatus = (String) meta.getOrDefault("status", "");

        // If already marked as missed checkout, skip
        if (currentStatus != null && currentStatus.contains("Checkout Not Done")) {
            return;
        }

        User user = userRepository.findById(att.getUserId()).orElse(null);
        if (user == null)
            return;

        OffsetDateTime shiftEndDateTime = calculateShiftEndTime(att, user);

        // Logic 1: Reminder (5 mins after shift end)
        if (now.isAfter(shiftEndDateTime.plusMinutes(5))) {
            boolean reminderSent = meta.containsKey("checkoutReminderSent")
                    && (boolean) meta.get("checkoutReminderSent");

            if (!reminderSent) {
                sendReminderEmail(user, shiftEndDateTime);
                meta.put("checkoutReminderSent", true);
                att.setMetadata(objectMapper.writeValueAsString(meta));
                attendanceRepository.save(att);
                return; // Wait for next cycle to mark as missed if needed
            }

            // Logic 2: Mark as Checkout Not Done (e.g., 30 mins after shift end)
            // If they still haven't checked out after reminder
            if (now.isAfter(shiftEndDateTime.plusMinutes(30))) {
                String finalStatus = String.format("Checkout Not Done — %s — Email Sent but User Did Not Checkout",
                        user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName()
                                : user.getUsername());

                meta.put("status", finalStatus);
                att.setMetadata(objectMapper.writeValueAsString(meta));
                attendanceRepository.save(att);
            }
        }
    }

    private void sendReminderEmail(User user, OffsetDateTime shiftEndDateTime) {
        String subject = "Action Required: Forgot to Checkout?";
        String body = String.format(
                "<p>Dear %s,</p>" +
                        "<p>This is a reminder that your shift ended at <strong>%s</strong>, but you have not marked your checkout yet.</p>"
                        +
                        "<p>Please login to the HR Portal and complete your checkout immediately to avoid attendance discrepancies.</p>"
                        +
                        "<p>Regards,<br>HR Team</p>",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                shiftEndDateTime.toLocalTime());
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    private OffsetDateTime calculateShiftEndTime(Attendance att, User user) {
        Shift shift = user.getShift();
        LocalTime endTime = LocalTime.of(18, 30); // Default 6:30 PM

        if (shift != null) {
            endTime = shift.getEndTime();
        }

        OffsetDateTime loginTime = att.getLoginTime();
        LocalTime loginLocalTime = loginTime.toLocalTime();

        // Handle Night Shift (Start > End)
        if (shift != null && shift.getStartTime().isAfter(shift.getEndTime())) {
            // Example: Start 22:00, End 06:00
            // If logged in at 23:00 (before midnight), end is tomorrow 06:00
            if (loginLocalTime.isBefore(LocalTime.MIDNIGHT)
                    && loginLocalTime.isAfter(shift.getStartTime().minusMinutes(60))) { // Tolerance
                return loginTime.toLocalDate().plusDays(1).atTime(endTime).atOffset(loginTime.getOffset());
            }
            // If logged in at 01:00 (after midnight), end is today 06:00
            return loginTime.toLocalDate().atTime(endTime).atOffset(loginTime.getOffset());
        }

        // Normal Shift
        return loginTime.toLocalDate().atTime(endTime).atOffset(loginTime.getOffset());
    }

    private Map<String, Object> getMetadata(Attendance att) throws Exception {
        if (att.getMetadata() == null || att.getMetadata().isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(att.getMetadata(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
    }
}
