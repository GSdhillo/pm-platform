package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberId.class)
@Getter @Setter @NoArgsConstructor
public class ProjectMember {
    @Id
    @Column(name = "project_id")
    private UUID projectId;
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    public ProjectMember(UUID projectId, UUID userId, Role role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.addedAt = Instant.now();
    }
}
