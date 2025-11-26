package com.megamart.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    List<Approval> findByStatus(String status);
    List<Approval> findByTargetUserId(UUID userId);
}
