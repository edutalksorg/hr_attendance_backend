package com.megamart.backend.teams;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;

    public Team create(String name, String description) {
        Team t = Team.builder().name(name).description(description).createdAt(OffsetDateTime.now()).build();
        return teamRepository.save(t);
    }

    public Team get(@NonNull UUID id) {
        return teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
    }

    public List<Team> list() {
        return teamRepository.findAll();
    }

    public Team update(UUID id, String name, String description) {
        Team t = get(id);
        t.setName(name);
        t.setDescription(description);
        return teamRepository.save(t);
    }

    public Team updateLeader(UUID id, UUID leaderId) {
        Team t = get(id);
        t.setLeaderId(leaderId);
        return teamRepository.save(t);
    }

    public void delete(@NonNull UUID id) {
        teamRepository.deleteById(id);
    }

    public TeamMember addMember(UUID teamId, UUID userId) {
        TeamMember m = TeamMember.builder().teamId(teamId).userId(userId).createdAt(OffsetDateTime.now()).build();
        return memberRepository.save(m);
    }

    public void removeMember(@NonNull UUID id) {
        memberRepository.deleteById(id);
    }

    public List<TeamMember> membersForTeam(UUID teamId) {
        return memberRepository.findByTeamId(teamId);
    }

    public List<TeamMember> teamsForUser(UUID userId) {
        return memberRepository.findByUserId(userId);
    }
}
