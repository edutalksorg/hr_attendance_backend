package com.megamart.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final PasswordEncoder encoder;

    public User registerEmployee(String fullName, String email, String phone, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        User u = User.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .password(encoder.encode(rawPassword))
                .role(UserRole.EMPLOYEE)
                .status(UserStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        userRepository.save(u);

        // create an approval entry
        Approval ap = Approval.builder()
                .targetUserId(u.getId())
                .approvalType("REGISTRATION")
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        approvalRepository.save(ap);
        return u;
    }

    public List<User> listAll() { return userRepository.findAll(); }

    public User findByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User approveUser(UUID approverId, UUID targetUserId, UserRole roleToSet) {
        User t = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("Target not found"));
        t.setRole(roleToSet);
        t.setStatus(UserStatus.ACTIVE);
        t.setApprovedBy(approverId);
        t.setApprovedAt(OffsetDateTime.now());
        userRepository.save(t);

        // update approval record(s)
        approvalRepository.findByTargetUserId(targetUserId).forEach(ap -> {
            ap.setStatus("APPROVED");
            ap.setApprovedBy(approverId);
            ap.setUpdatedAt(OffsetDateTime.now());
            approvalRepository.save(ap);
        });
        return t;
    }

    public void blockUser(UUID userId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setStatus(UserStatus.BLOCKED);
        userRepository.save(u);
    }
}
