package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "issue_watchers")
@IdClass(IssueWatcherId.class)
@Getter @Setter @NoArgsConstructor
public class IssueWatcher {
    @Id
    @Column(name = "issue_id")
    private UUID issueId;
    @Id
    @Column(name = "user_id")
    private UUID userId;

    public IssueWatcher(UUID issueId, UUID userId) { this.issueId = issueId; this.userId = userId; }
}
