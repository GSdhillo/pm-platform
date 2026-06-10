package com.gurjeet.pm.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProjectMemberId implements Serializable {
    private UUID projectId;
    private UUID userId;

    public ProjectMemberId() {}
    public ProjectMemberId(UUID projectId, UUID userId) { this.projectId = projectId; this.userId = userId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMemberId that)) return false;
        return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
    }
    @Override public int hashCode() { return Objects.hash(projectId, userId); }
}
