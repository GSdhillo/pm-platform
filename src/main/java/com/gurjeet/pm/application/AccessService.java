package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.ProjectMemberRepository;
import com.gurjeet.pm.common.error.ForbiddenException;
import com.gurjeet.pm.domain.model.ProjectMember;
import com.gurjeet.pm.domain.model.ProjectMemberId;
import com.gurjeet.pm.domain.model.Role;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccessService {
    private final ProjectMemberRepository memberRepository;

    public AccessService(ProjectMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public ProjectMember requireRole(UUID projectId, UUID userId, Role minimum) {
        ProjectMember member = memberRepository.findById(new ProjectMemberId(projectId, userId))
                .orElseThrow(() -> new ForbiddenException("You are not a member of this project"));
        if (member.getRole().rank() < minimum.rank()) {
            throw new ForbiddenException("This operation requires role " + minimum + " or higher (you are " + member.getRole() + ")");
        }
        return member;
    }

    public List<UUID> visibleProjectIds(UUID userId) {
        return memberRepository.findProjectIdsByUserId(userId);
    }
}
