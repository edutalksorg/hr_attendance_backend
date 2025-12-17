package com.megamart.backend.teams;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.profile.UserProfileService;
import com.megamart.backend.profile.UserProfileEntity;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TeamController {
    private final TeamService service;
    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    public static record CreateReq(@NotBlank String name, String description) {
    }

    public static record UpdateReq(@NotBlank String name, String description) {
    }

    public static record AssignLeaderReq(@NotNull UUID userId) {
    }

    public static record AddMemberReq(@NotNull UUID userId) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Team> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(201).body(service.create(req.name(), req.description()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Team> get(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<Team>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Team> update(@PathVariable @NonNull UUID id, @Valid @RequestBody UpdateReq req) {
        return ResponseEntity.ok(service.update(id, req.name(), req.description()));
    }

    @PutMapping("/{id}/leader")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Team> assignLeader(@PathVariable @NonNull UUID id, @Valid @RequestBody AssignLeaderReq req) {
        return ResponseEntity.ok(service.updateLeader(id, req.userId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<TeamMember> addMember(@PathVariable @NonNull UUID id, @Valid @RequestBody AddMemberReq req) {
        return ResponseEntity.status(201).body(service.addMember(id, req.userId()));
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TeamController.class);

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<TeamMember>> members(@PathVariable @NonNull UUID id) {
        logger.info("Fetching members for team: {}", id);
        try {
            List<TeamMember> members = service.membersForTeam(id);
            if (members == null) {
                logger.warn("Service returned null members list for team {}", id);
                return ResponseEntity.ok(List.of());
            }

            members.forEach(m -> {
                try {
                    if (m.getUserId() == null) {
                        return;
                    }
                    User user = userRepository.findById(m.getUserId()).orElse(null);
                    if (user != null) {
                        m.setUserName(user.getFullName());
                        if (user.getRole() != null) {
                            m.setUserRole(user.getRole().toString());
                        }
                        m.setUserEmployeeId(user.getEmployeeId());

                        try {
                            UserProfileEntity profile = userProfileService.getProfile(user.getId());
                            if (profile != null) {
                                m.setUserProfilePhoto(profile.getPhotoUrl());
                                if (m.getUserName() == null || m.getUserName().isEmpty()) {
                                    m.setUserName(profile.getUsername());
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to fetch profile for user {}: {}", user.getId(),
                                    e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error enriching member data for member {}", m.getId(), e);
                }
            });
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            logger.error("Error fetching team members for team " + id, e);
            // Return empty list instead of crashing
            return ResponseEntity.ok(List.of());
        }
    }

    @DeleteMapping("/members/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> removeMember(@PathVariable @NonNull UUID memberId) {
        service.removeMember(memberId);
        return ResponseEntity.ok().build();
    }
}
