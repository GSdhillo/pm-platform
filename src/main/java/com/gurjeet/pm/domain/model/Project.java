package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor
public class Project {
    @Id
    private UUID id;
    @Column(name = "project_key", nullable = false, unique = true)
    private String projectKey;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(name = "lead_id")
    private UUID leadId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Project(String projectKey, String name, String description, UUID leadId) {
        this.id = UUID.randomUUID();
        this.projectKey = projectKey;
        this.name = name;
        this.description = description;
        this.leadId = leadId;
        this.createdAt = Instant.now();
    }
}
