package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "workflow_transitions")
@Getter @Setter @NoArgsConstructor
public class WorkflowTransition {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;
    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;
    private String name;

    public WorkflowTransition(UUID projectId, UUID fromStatusId, UUID toStatusId, String name) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.fromStatusId = fromStatusId;
        this.toStatusId = toStatusId;
        this.name = name;
    }
}
