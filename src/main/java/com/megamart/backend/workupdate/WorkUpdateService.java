package com.megamart.backend.workupdate;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRole;
import com.megamart.backend.shift.Shift;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkUpdateService {

    private final WorkUpdateRepository workUpdateRepository;

    @Transactional
    public WorkUpdate createWorkUpdate(User user, String description) {
        LocalDate today = LocalDate.now();
        if (workUpdateRepository.findByUserIdAndDateAndDeletedAtIsNull(user.getId(), today).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work update already submitted for today.");
        }

        Shift shift = user.getShift();
        if (shift == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No shift assigned to user.");
        }

        if (user.getRole() == UserRole.EMPLOYEE || user.getRole() == UserRole.MARKETING_EXECUTIVE) {
            validateShift(shift);
        }

        WorkUpdate workUpdate = WorkUpdate.builder()
                .user(user)
                .role(user.getRole())
                .branch(user.getBranch())
                .date(today)
                .shift(shift)
                .workDescription(description)
                .build();

        return workUpdateRepository.save(workUpdate);
    }

    private void validateShift(Shift shift) {
        LocalTime now = LocalTime.now();
        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();

        boolean isActive;
        if (start.isBefore(end)) {
            isActive = !now.isBefore(start) && !now.isAfter(end);
        } else {
            isActive = !now.isBefore(start) || !now.isAfter(end);
        }

        if (!isActive) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only update work during your active shift.");
        }
    }

    public List<WorkUpdate> getMyUpdates(User user) {
        return workUpdateRepository.findAllByFilters(user.getId(), null, null, null, null);
    }

    public WorkUpdate getTodayUpdate(User user) {
        return workUpdateRepository.findByUserIdAndDateAndDeletedAtIsNull(user.getId(), LocalDate.now())
                .orElse(null);
    }

    public List<WorkUpdate> getAllUpdates(UUID userId, Integer month, Integer year, UUID branchId, UserRole role) {
        return workUpdateRepository.findAllByFilters(userId, month, year, branchId, role);
    }

    @Transactional
    public void deleteUpdate(UUID id, User deleter) {
        if (isRestrictedRole(deleter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to delete.");
        }

        WorkUpdate update = workUpdateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Update not found"));

        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(deleter.getId());
        workUpdateRepository.save(update);
    }

    @Transactional
    public void deleteBulk(List<UUID> ids, User deleter) {
        if (isRestrictedRole(deleter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to delete.");
        }
        List<WorkUpdate> updates = workUpdateRepository.findAllById(ids);
        OffsetDateTime now = OffsetDateTime.now();
        for (WorkUpdate w : updates) {
            w.setDeletedAt(now);
            w.setDeletedBy(deleter.getId());
        }
        workUpdateRepository.saveAll(updates);
    }

    private boolean isRestrictedRole(User user) {
        return user.getRole() == UserRole.EMPLOYEE || user.getRole() == UserRole.MARKETING_EXECUTIVE;
    }
}
