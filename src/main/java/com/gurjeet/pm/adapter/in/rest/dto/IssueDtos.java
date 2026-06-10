package com.gurjeet.pm.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.gurjeet.pm.domain.model.Issue;
import com.gurjeet.pm.domain.model.IssueType;
import com.gurjeet.pm.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IssueDtos {
    private IssueDtos() {}

    public record CreateIssueRequest(
            @NotNull IssueType type,
            @NotBlank @Size(max = 300) String title,
            @Size(max = 10000) String description,
            Priority priority,
            UUID assigneeId,
            UUID sprintId,
            UUID parentId,
            Integer storyPoints,
            List<String> labels,
            Map<UUID, JsonNode> customFields) {}

    public record UpdateIssueRequest(
            @NotNull Long expectedVersion,
            String title,
            String description,
            Priority priority,
            JsonNode assigneeId,
            JsonNode sprintId,
            JsonNode parentId,
            JsonNode storyPoints,
            List<String> labels,
            Map<UUID, JsonNode> customFields) {}

    public record TransitionRequest(@NotNull UUID toStatusId, @NotNull Long expectedVersion) {}

    public record IssueResponse(UUID id, String key, IssueType type, String title, String description,
                                UUID statusId, Priority priority, long version, UUID assigneeId,
                                UUID reporterId, UUID sprintId, UUID parentId, Integer storyPoints,
                                List<String> labels, Instant createdAt, Instant updatedAt) {
        public static IssueResponse from(Issue i) {
            return new IssueResponse(i.getId(), i.getIssueKey(), i.getType(), i.getTitle(), i.getDescription(),
                    i.getStatusId(), i.getPriority(), i.getVersion(), i.getAssigneeId(),
                    i.getReporterId(), i.getSprintId(), i.getParentId(), i.getStoryPoints(),
                    i.getLabels(), i.getCreatedAt(), i.getUpdatedAt());
        }
    }
}
