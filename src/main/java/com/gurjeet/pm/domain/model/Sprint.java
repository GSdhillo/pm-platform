package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sprints")
@Getter @Setter @NoArgsConstructor
public class Sprint {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(nullable = false)
    private String name;
    private String goal;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status = SprintStatus.FUTURE;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "completed_points")
    private Integer completedPoints;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Sprint(UUID projectId, String name, String goal, LocalDate startDate, LocalDate endDate) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = Instant.now();
    }
}
