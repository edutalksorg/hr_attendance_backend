package com.megamart.backend.branch;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BranchController {
    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<Branch> getAllBranches() {
        return branchService.getAllBranches();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public Branch getBranchById(@PathVariable UUID id) {
        return branchService.getBranchById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Branch createBranch(@RequestBody Branch branch) {
        return branchService.createBranch(branch);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Branch updateBranch(@PathVariable UUID id, @RequestBody Branch branch) {
        return branchService.updateBranch(id, branch);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public List<com.megamart.backend.user.User> getBranchUsers(@PathVariable UUID id) {
        return branchService.getBranchUsers(id);
    }

    @GetMapping("/unassigned-users")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public List<com.megamart.backend.user.User> getUnassignedUsers() {
        return branchService.getUnassignedUsers();
    }

    @PutMapping("/{id}/assign-users")
    @PreAuthorize("hasRole('ADMIN')")
    public void assignUsers(@PathVariable UUID id, @RequestBody List<UUID> userIds) {
        branchService.assignUsersToBranch(id, userIds);
    }

    @PutMapping("/unassign-users")
    @PreAuthorize("hasRole('ADMIN')")
    public void unassignUsers(@RequestBody List<UUID> userIds) {
        branchService.unassignUsers(userIds);
    }

    @PutMapping("/{id}/transfer-user")
    @PreAuthorize("hasRole('ADMIN')")
    public void transferUser(@PathVariable UUID id, @RequestParam UUID userId) {
        branchService.transferUserToBranch(id, userId);
    }
}
