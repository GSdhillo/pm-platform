package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.ProjectMember;
import com.gurjeet.pm.domain.model.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByProjectId(UUID projectId);
    List<ProjectMember> findByUserId(UUID userId);

    @Query("select m.projectId from ProjectMember m where m.userId = :userId")
    List<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);
}
