package com.megamart.backend.teams;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService service;

    public static record CreateReq(@NotBlank String name, String description) {}
    public static record UpdateReq(@NotBlank String name, String description) {}
    public static record AddMemberReq(@NotNull UUID userId) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Team> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(201).body(service.create(req.name(), req.description()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<Team> get(@PathVariable UUID id) { return ResponseEntity.ok(service.get(id)); }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<Team>> list() { return ResponseEntity.ok(service.list()); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Team> update(@PathVariable UUID id, @Valid @RequestBody UpdateReq req) { return ResponseEntity.ok(service.update(id, req.name(), req.description())); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.ok().build(); }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<TeamMember> addMember(@PathVariable UUID id, @Valid @RequestBody AddMemberReq req) { return ResponseEntity.status(201).body(service.addMember(id, req.userId())); }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<TeamMember>> members(@PathVariable UUID id) { return ResponseEntity.ok(service.membersForTeam(id)); }

    @DeleteMapping("/members/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> removeMember(@PathVariable UUID memberId) { service.removeMember(memberId); return ResponseEntity.ok().build(); }
}
