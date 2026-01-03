package com.megamart.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findByRole(com.megamart.backend.user.UserRole role);

    long countByRole(UserRole role);

    long countByRoleAndBranchId(UserRole role, UUID branchId);

    long countByBranchId(UUID branchId);

    java.util.List<User> findByBranchId(UUID branchId);

    java.util.List<User> findByBranchIsNull();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.lastLogin = :lastLogin, u.lastIp = :lastIp WHERE u.id = :id")
    void updateLoginMetadata(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("lastLogin") java.time.OffsetDateTime lastLogin,
            @org.springframework.data.repository.query.Param("lastIp") String lastIp);
}
