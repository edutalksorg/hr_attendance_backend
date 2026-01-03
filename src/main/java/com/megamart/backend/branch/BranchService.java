package com.megamart.backend.branch;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branchRepository;
    private final com.megamart.backend.user.UserRepository userRepository;
    private final com.megamart.backend.notification.NotificationService notificationService;

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Branch getBranchById(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
    }

    @Transactional
    public Branch createBranch(Branch branch) {
        branch.setCreatedAt(OffsetDateTime.now());
        branch.setUpdatedAt(OffsetDateTime.now());
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(UUID id, Branch branchDetails) {
        Branch branch = getBranchById(id);
        branch.setName(branchDetails.getName());
        branch.setCode(branchDetails.getCode());
        branch.setAddress(branchDetails.getAddress());
        branch.setLatitude(branchDetails.getLatitude());
        branch.setLongitude(branchDetails.getLongitude());
        branch.setGeoRadius(branchDetails.getGeoRadius());
        branch.setUpdatedAt(OffsetDateTime.now());
        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(UUID id) {
        branchRepository.deleteById(id);
    }

    public List<com.megamart.backend.user.User> getBranchUsers(UUID branchId) {
        return userRepository.findByBranchId(branchId);
    }

    public List<com.megamart.backend.user.User> getUnassignedUsers() {
        return userRepository.findByBranchIsNull();
    }

    @Transactional
    public void assignUsersToBranch(UUID branchId, List<UUID> userIds) {
        Branch branch = getBranchById(branchId);
        List<com.megamart.backend.user.User> users = userRepository.findAllById(userIds);
        for (com.megamart.backend.user.User user : users) {
            user.setBranch(branch);
            user.setUpdatedAt(OffsetDateTime.now());
            notificationService.send(user.getId(), "Branch Assignment", "You have been assigned to " + branch.getName(),
                    "INFO");
        }
        userRepository.saveAll(users);
    }

    @Transactional
    public void unassignUsers(List<UUID> userIds) {
        List<com.megamart.backend.user.User> users = userRepository.findAllById(userIds);
        for (com.megamart.backend.user.User user : users) {
            user.setBranch(null);
            user.setUpdatedAt(OffsetDateTime.now());
            notificationService.send(user.getId(), "Branch Unassignment",
                    "You have been unassigned from your current branch.",
                    "INFO");
        }
        userRepository.saveAll(users);
    }

    @Transactional
    public void transferUserToBranch(UUID branchId, UUID userId) {
        Branch branch = getBranchById(branchId);
        com.megamart.backend.user.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBranch(branch);
        user.setUpdatedAt(OffsetDateTime.now());
        notificationService.send(user.getId(), "Branch Transfer", "You have been transferred to " + branch.getName(),
                "INFO");
        userRepository.save(user);
    }
}
