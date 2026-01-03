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
    private final com.megamart.backend.branch.BranchRepository branchRepository;

    public User registerEmployee(String fullName, String email, String phone, String rawPassword, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        User u = User.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .password(encoder.encode(rawPassword))
                .role(role)
                .status(UserStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(u);
    }

    public List<User> listAll() {
        try {
            List<User> users = userRepository.findAll();
            populatePhotos(users);
            return users;
        } catch (Exception e) {
            logger.error("Error listing all users", e);
            throw e;
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
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User approveUser(@NonNull UUID approverId, @NonNull UUID targetUserId) {
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (approver.getRole() == UserRole.HR || approver.getRole() == UserRole.MANAGER) {
            if (target.getRole() != UserRole.EMPLOYEE && target.getRole() != UserRole.MARKETING_EXECUTIVE) {
                throw new RuntimeException("You can only approve Employee and Marketing roles.");
            }
        }

        target.setStatus(UserStatus.ACTIVE);
        target.setApprovedBy(approverId);
        target.setApprovedAt(OffsetDateTime.now());
        userRepository.save(target);

        approvalRepository.findByTargetUserId(targetUserId).forEach(ap -> {
            ap.setStatus("APPROVED");
            ap.setApprovedBy(approverId);
            ap.setUpdatedAt(OffsetDateTime.now());
            approvalRepository.save(ap);
        });

        notificationService.send(targetUserId, "Account Approved", "Your account has been approved.", "SUCCESS");
        return target;
    }

    public void blockUser(@NonNull UUID userId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setStatus(UserStatus.BLOCKED);
        u.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(u);
        notificationService.send(userId, "Account Blocked", "Your account has been blocked by admin.", "ERROR");
    }

    @org.springframework.transaction.annotation.Transactional
    public void unblockUser(UUID id) {
        User u = userRepository.findById(id).orElseThrow();
        u.setStatus(UserStatus.ACTIVE);
        u.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(u);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public User transferUserToBranch(@NonNull UUID userId, @NonNull UUID branchId) {
        User user = userRepository.findById(userId).orElseThrow();
        com.megamart.backend.branch.Branch branch = branchRepository.findById(branchId).orElseThrow();
        user.setBranch(branch);
        user.setUpdatedAt(OffsetDateTime.now());
        notificationService.send(userId, "Branch Transfer", "You have been transferred to " + branch.getName(), "INFO");
        return userRepository.save(user);
    }

    public List<User> findByBranch(UUID branchId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getBranch() != null && u.getBranch().getId().equals(branchId))
                .collect(java.util.stream.Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional
    public User updateGeoRestriction(UUID id, com.megamart.backend.dto.GeoRestrictionRequest request) {
        User u = userRepository.findById(id).orElseThrow();
        u.setGeoRestrictionEnabled(request.isEnabled());
        u.setOfficeLatitude(request.getLatitude());
        u.setOfficeLongitude(request.getLongitude());
        u.setGeoRadius(request.getRadius());
        u.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(u);
    }

    @org.springframework.transaction.annotation.Transactional
    public void bulkUpdateGeoRestriction(List<UUID> userIds, com.megamart.backend.dto.GeoRestrictionRequest data) {
        List<User> users = userRepository.findAllById(userIds);
        users.forEach(u -> {
            u.setGeoRestrictionEnabled(data.isEnabled());
            u.setOfficeLatitude(data.getLatitude());
            u.setOfficeLongitude(data.getLongitude());
            u.setGeoRadius(data.getRadius());
            u.setUpdatedAt(OffsetDateTime.now());
        });
        userRepository.saveAll(users);
    }

    @org.springframework.transaction.annotation.Transactional
    public User updateJoiningDate(UUID userId, java.time.LocalDate date) {
        User u = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        u.setJoiningDate(date);
        u.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(u);
    }
}
