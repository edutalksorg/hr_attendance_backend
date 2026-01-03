package com.megamart.backend.workupdate;

import com.megamart.backend.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkUpdateRepository extends JpaRepository<WorkUpdate, UUID> {

    Optional<WorkUpdate> findByUserIdAndDateAndDeletedAtIsNull(UUID userId, LocalDate date);

    @Query("SELECT w FROM WorkUpdate w WHERE w.deletedAt IS NULL AND " +
            "(:userId IS NULL OR w.user.id = :userId) AND " +
            "(:month IS NULL OR MONTH(w.date) = :month) AND " +
            "(:year IS NULL OR YEAR(w.date) = :year) AND " +
            "(:branchId IS NULL OR w.branch.id = :branchId) AND " +
            "(:role IS NULL OR w.role = :role)")
    List<WorkUpdate> findAllByFilters(@Param("userId") UUID userId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("branchId") UUID branchId,
            @Param("role") UserRole role);

    List<WorkUpdate> findByDeletedAtIsNull();
}
