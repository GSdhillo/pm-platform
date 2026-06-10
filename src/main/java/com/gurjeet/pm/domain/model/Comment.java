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
@Table(name = "comments")
@Getter @Setter @NoArgsConstructor
public class Comment {
    @Id
    private UUID id;
    @Column(name = "issue_id", nullable = false)
    private UUID issueId;
    @Column(name = "parent_comment_id")
    private UUID parentCommentId;
    @Column(name = "author_id", nullable = false)
    private UUID authorId;
    @Column(nullable = false)
    private String body;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> mentions = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Comment(UUID issueId, UUID parentCommentId, UUID authorId, String body, List<String> mentions) {
        this.id = UUID.randomUUID();
        this.issueId = issueId;
        this.parentCommentId = parentCommentId;
        this.authorId = authorId;
        this.body = body;
        if (mentions != null) this.mentions = mentions;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}
