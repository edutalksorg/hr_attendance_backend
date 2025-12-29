package com.megamart.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import com.megamart.backend.profile.UserProfileEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final PasswordEncoder encoder;
    private final com.megamart.backend.profile.UserProfileRepository userProfileRepository;
    private final com.megamart.backend.notification.NotificationService notificationService;

    public User registerEmployee(String fullName, String email, String phone, String rawPassword, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        User u = User.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .password(encoder.encode(rawPassword))
                .role(role) // Use the provided role instead of hardcoding EMPLOYEE
                .status(UserStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        userRepository.save(u);

        // create an approval entry with the requested role
        Approval ap = Approval.builder()
                .targetUserId(u.getId())
                .approvalType("REGISTRATION")
                .roleAfter(role) // Store the requested role for admin reference
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        approvalRepository.save(ap);
        return u;
    }

    public List<User> listAll() {
        try {
            List<User> users = userRepository.findAll();
            populatePhotos(users);
            return users;
        } catch (Exception e) {
            logger.error("Error listing all users", e);
            throw e; // Rethrow to let the controller handle it (or return empty list if preferred)
        }
    }

    public List<User> findByRole(UserRole role) {
        List<User> users = userRepository.findByRole(role);
        populatePhotos(users);
        return users;
    }

    private void populatePhotos(List<User> users) {
        if (users == null || users.isEmpty())
            return;
        try {
            List<UserProfileEntity> profiles = userProfileRepository.findAll();
            Map<UUID, String> photoMap = new java.util.HashMap<>();
            for (UserProfileEntity p : profiles) {
                if (p.getUserId() != null) {
                    photoMap.put(p.getUserId(), p.getPhotoUrl() != null ? p.getPhotoUrl() : "");
                }
            }

            users.forEach(u -> {
                if (u.getId() != null && photoMap.containsKey(u.getId())) {
                    u.setProfilePhoto(photoMap.get(u.getId()));
                }
            });
        } catch (Exception e) {
            logger.error("Error populating user photos", e);
            // Non-critical, swallow exception to return user data at least
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User approveUser(@NonNull UUID approverId, @NonNull UUID targetUserId) {
        // Get approver and target user
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        // ⚠️ CRITICAL: Role-Based Approval Validation
        // Check the target user's EXISTING role (not changing it)
        if (approver.getRole() == UserRole.HR) {
            // HR can ONLY approve EMPLOYEE and MARKETING roles
            if (target.getRole() != UserRole.EMPLOYEE && target.getRole() != UserRole.MARKETING_EXECUTIVE) {
                throw new RuntimeException("HR can only approve Employee and Marketing roles.");
            }

            // HR cannot approve users with ADMIN or HR role
            if (target.getRole() == UserRole.ADMIN || target.getRole() == UserRole.HR) {
                throw new RuntimeException("You do not have permission to approve this role.");
            }
        }

        // ADMIN can approve all roles (no restrictions)
        // If approver is ADMIN, all approvals are allowed

        // ✅ DO NOT CHANGE ROLE - Only update status to ACTIVE
        // The role was already set correctly during registration
        target.setStatus(UserStatus.ACTIVE);
        target.setApprovedBy(approverId);
        target.setApprovedAt(OffsetDateTime.now());
        userRepository.save(target);

        // Update approval record(s)
        approvalRepository.findByTargetUserId(targetUserId).forEach(ap -> {
            ap.setStatus("APPROVED");
            ap.setApprovedBy(approverId);
            ap.setUpdatedAt(OffsetDateTime.now());
            approvalRepository.save(ap);
        });

        // Notify
        notificationService.send(targetUserId, "Account Approved", "Your account has been approved. You can now login.",
                "SUCCESS");

        return target;
    }

    public void blockUser(@NonNull UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setStatus(UserStatus.BLOCKED);
        u.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(u);
        notificationService.send(userId, "Account Blocked", "Your account has been blocked by admin.", "ERROR");
    }

    public void unblockUser(@NonNull UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setStatus(UserStatus.ACTIVE);
        u.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(u);
        notificationService.send(userId, "Account Unblocked", "Your account has been unblocked.", "SUCCESS");
    }

    public void deleteUser(@NonNull UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete all related approval records first
        approvalRepository.findByTargetUserId(userId).forEach(approvalRepository::delete);

        // Delete the user completely from database
        userRepository.delete(u);
    }
}
