package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "workflow_statuses")
@Getter @Setter @NoArgsConstructor
public class WorkflowStatus {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCategory category;
    @Column(nullable = false)
    private int position;
    @Column(name = "wip_limit")
    private Integer wipLimit;

    public WorkflowStatus(UUID projectId, String name, StatusCategory category, int position, Integer wipLimit) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.name = name;
        this.category = category;
        this.position = position;
        this.wipLimit = wipLimit;
    }
}
