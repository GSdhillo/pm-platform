package com.gurjeet.pm.domain.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "issues")
@Getter @Setter @NoArgsConstructor
public class Issue {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "issue_key", nullable = false, unique = true)
    private String issueKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueType type;
    @Column(nullable = false)
    private String title;
    private String description;
    @Column(name = "status_id", nullable = false)
    private UUID statusId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "assignee_id")
    private UUID assigneeId;
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;
    @Column(name = "sprint_id")
    private UUID sprintId;
    @Column(name = "parent_id")
    private UUID parentId;
    @Column(name = "story_points")
    private Integer storyPoints;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> labels = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Issue(UUID projectId, String issueKey, IssueType type, String title, UUID statusId, UUID reporterId) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.issueKey = issueKey;
        this.type = type;
        this.title = title;
        this.statusId = statusId;
        this.reporterId = reporterId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }
}
