package com.megamart.backend.performance;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.teams.TeamMemberRepository;
import com.megamart.backend.teams.TeamMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final com.megamart.backend.email.EmailService emailService;
    private final PerformanceGoalRepository goalRepository;
    private final PerformanceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    public List<PerformanceGoal> getUserGoals(UUID userId) {
        return goalRepository.findByUserId(userId);
    }

    public PerformanceGoal createGoal(UUID userId, PerformanceGoal goal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        goal.setUser(user);
        PerformanceGoal savedGoal = goalRepository.save(goal);

        // Notify user
        String subject = "New Performance Goal Assigned: "
                + (savedGoal.getTitle() != null ? savedGoal.getTitle() : "Performance Goal");
        String body = String.format("""
                <div style="font-family: Arial, sans-serif; color: #333;">
                    <h2>New Goal Assigned</h2>
                    <p>Dear %s,</p>
                    <p>A new performance goal has been assigned to you.</p>
                    <div style="background: #f9f9f9; padding: 15px; border-left: 4px solid #3b82f6; margin: 20px 0;">
                        <h3 style="margin: 0 0 10px;">%s</h3>
                        <p style="margin: 0;">%s</p>
                        <p style="margin-top: 10px; font-size: 0.9em; color: #666;">Due Date: %s</p>
                    </div>
                    <p>Please login to the HR Portal to track your progress.</p>
                    <p>Best regards,<br/>HR Team</p>
                </div>
                """,
                user.getFullName() != null ? user.getFullName() : "Employee",
                savedGoal.getTitle() != null ? savedGoal.getTitle() : "Untitled Goal",
                savedGoal.getDescription() != null ? savedGoal.getDescription() : "No description provided.",
                savedGoal.getEndDate() != null ? savedGoal.getEndDate().toString() : "TBD");

        emailService.sendEmail(user.getEmail(), subject, body);

        return savedGoal;
    }

    public PerformanceGoal updateGoal(UUID id, PerformanceGoal updates) {
        PerformanceGoal goal = goalRepository.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));

        if (updates.getTitle() != null)
            goal.setTitle(updates.getTitle());
        if (updates.getDescription() != null)
            goal.setDescription(updates.getDescription());
        if (updates.getType() != null)
            goal.setType(updates.getType());
        if (updates.getStartDate() != null)
            goal.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null)
            goal.setEndDate(updates.getEndDate());

        if (updates.getStatus() != null)
            goal.setStatus(updates.getStatus());
        if (updates.getProgressPercentage() != null)
            goal.setProgressPercentage(updates.getProgressPercentage());
        if (updates.getAdminFeedback() != null)
            goal.setAdminFeedback(updates.getAdminFeedback());
        return goalRepository.save(goal);
    }

    public List<PerformanceReview> getReviews(UUID userId) {
        return reviewRepository.findByUser_Id(userId);
    }

    public List<PerformanceReview> getReviewsForUsers(List<UUID> userIds) {
        return reviewRepository.findByUser_IdIn(userIds);
    }

    public List<PerformanceReview> getReviewsForTeam(UUID teamId) {
        List<UUID> userIds = teamMemberRepository.findByTeamId(teamId).stream()
                .map(TeamMember::getUserId)
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return reviewRepository.findByUser_IdIn(userIds);
    }

    public List<PerformanceReview> getReviewsByFilter(UUID teamId, List<UUID> userIds) {
        java.util.Set<UUID> finalUserIds = new java.util.HashSet<>();

        if (userIds != null) {
            finalUserIds.addAll(userIds);
        }

        if (teamId != null) {
            teamMemberRepository.findByTeamId(teamId).forEach(member -> finalUserIds.add(member.getUserId()));
        }

        if (finalUserIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return reviewRepository.findByUser_IdIn(new java.util.ArrayList<>(finalUserIds));
    }

    public List<PerformanceReview> getAllReviews() {
        return reviewRepository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public PerformanceReview addReview(UUID userId, UUID reviewerId, PerformanceReview review) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        review.setUser(user);
        review.setReviewer(reviewer);
        review.setReviewDate(java.time.LocalDate.now());

        return reviewRepository.save(review);
    }

    public void deleteReview(UUID reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Review not found");
        }
        reviewRepository.deleteById(reviewId);
    }

    public void deleteGoal(UUID goalId) {
        PerformanceGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        goalRepository.delete(goal);
    }

    // Stats or aggregations can be added here
}
