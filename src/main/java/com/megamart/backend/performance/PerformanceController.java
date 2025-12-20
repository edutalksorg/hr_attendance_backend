package com.megamart.backend.performance;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final com.megamart.backend.user.UserService userService;

    @GetMapping("/goals/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<PerformanceGoal>> getUserGoals(@PathVariable UUID userId) {
        return ResponseEntity.ok(performanceService.getUserGoals(userId));
    }

    @PostMapping("/goals/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<PerformanceGoal> createGoal(@PathVariable UUID userId, @RequestBody PerformanceGoal goal) {
        return ResponseEntity.ok(performanceService.createGoal(userId, goal));
    }

    @PutMapping("/goals/{goalId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<PerformanceGoal> updateGoal(@PathVariable UUID goalId, @RequestBody PerformanceGoal goal) {
        // Validation logic for permissions can be added (e.g. Employee can only update
        // progress)
        return ResponseEntity.ok(performanceService.updateGoal(goalId, goal));
    }

    @DeleteMapping("/goals/{goalId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> deleteGoal(@PathVariable UUID goalId) {
        performanceService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<PerformanceReview>> getUserReviews(@PathVariable UUID userId) {
        return ResponseEntity.ok(performanceService.getReviews(userId));
    }

    @GetMapping("/reviews/all")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<PerformanceReview>> getAllReviews() {
        return ResponseEntity.ok(performanceService.getAllReviews());
    }

    @PostMapping("/reviews/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<PerformanceReview> addReview(
            @PathVariable UUID userId,
            @RequestBody PerformanceReview review) {

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        com.megamart.backend.user.User reviewer = this.userService.findByEmail(email);

        return ResponseEntity.ok(performanceService.addReview(userId, reviewer.getId(), review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
        performanceService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/filter")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<PerformanceReview>> filterReviews(@RequestBody ReviewFilterRequest request) {
        return ResponseEntity.ok(performanceService.getReviewsByFilter(request.teamId(), request.userIds()));
    }

    public static record ReviewFilterRequest(UUID teamId, List<UUID> userIds) {
    }
}
