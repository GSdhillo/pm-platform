package com.gurjeet.pm.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IssueWatcherId implements Serializable {
    private UUID issueId;
    private UUID userId;

    public IssueWatcherId() {}
    public IssueWatcherId(UUID issueId, UUID userId) { this.issueId = issueId; this.userId = userId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueWatcherId that)) return false;
        return Objects.equals(issueId, that.issueId) && Objects.equals(userId, that.userId);
    }
    @Override public int hashCode() { return Objects.hash(issueId, userId); }
}
