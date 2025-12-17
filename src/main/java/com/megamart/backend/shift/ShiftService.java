package com.megamart.backend.shift;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ShiftService {
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final com.megamart.backend.notification.NotificationService notificationService;

    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    public Shift createShift(Shift shift) {
        if (shiftRepository.existsByName(shift.getName())) {
            throw new RuntimeException("Shift with this name already exists");
        }
        return shiftRepository.save(shift);
    }

    public void deleteShift(UUID id) {
        shiftRepository.deleteById(id);
    }

    public User assignShift(UUID userId, UUID shiftId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String notifTitle;
        String notifMsg;

        if (shiftId != null) {
            Shift shift = shiftRepository.findById(shiftId).orElseThrow(() -> new RuntimeException("Shift not found"));
            user.setShift(shift);
            notifTitle = "Shift Assigned";
            notifMsg = "You have been assigned to shift: " + shift.getName() + " (" + shift.getStartTime() + " - "
                    + shift.getEndTime() + ")";
        } else {
            user.setShift(null);
            notifTitle = "Shift Removed";
            notifMsg = "Your shift assignment has been removed.";
        }
        User saved = userRepository.save(user);

        notificationService.send(userId, notifTitle, notifMsg, "INFO");

        return saved;
    }

    public Shift updateShift(UUID id, Shift shiftDetails) {
        Shift shift = shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Shift not found"));
        shift.setName(shiftDetails.getName());
        shift.setStartTime(shiftDetails.getStartTime());
        shift.setEndTime(shiftDetails.getEndTime());
        shift.setLateGraceMinutes(shiftDetails.getLateGraceMinutes());
        shift.setHalfDayTime(shiftDetails.getHalfDayTime());
        shift.setAbsentTime(shiftDetails.getAbsentTime());
        shift.setLateCountLimit(shiftDetails.getLateCountLimit());
        return shiftRepository.save(shift);
    }
}
