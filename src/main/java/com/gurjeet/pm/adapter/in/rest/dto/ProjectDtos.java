package com.gurjeet.pm.adapter.in.rest.dto;

import com.gurjeet.pm.domain.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record CreateProjectRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{2,10}", message = "2-10 letters") String key,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description) {}

    public record ProjectResponse(UUID id, String key, String name, String description, UUID leadId, Instant createdAt) {
        public static ProjectResponse from(Project p) {
            return new ProjectResponse(p.getId(), p.getProjectKey(), p.getName(), p.getDescription(), p.getLeadId(), p.getCreatedAt());
        }
    }

    public record AddMemberRequest(@NotBlank String email, @NotNull Role role) {}
    public record ChangeRoleRequest(@NotNull Role role) {}
    public record MemberResponse(UUID userId, Role role, Instant addedAt) {
        public static MemberResponse from(ProjectMember m) { return new MemberResponse(m.getUserId(), m.getRole(), m.getAddedAt()); }
    }

    public record AddStatusRequest(@NotBlank String name, @NotNull StatusCategory category, Integer wipLimit) {}
    public record StatusResponse(UUID id, String name, StatusCategory category, int position, Integer wipLimit) {
        public static StatusResponse from(WorkflowStatus s) {
            return new StatusResponse(s.getId(), s.getName(), s.getCategory(), s.getPosition(), s.getWipLimit());
        }
    }

    public record AddTransitionRequest(@NotNull UUID fromStatusId, @NotNull UUID toStatusId, String name) {}
    public record TransitionResponse(UUID id, UUID fromStatusId, UUID toStatusId, String name) {
        public static TransitionResponse from(WorkflowTransition t) {
            return new TransitionResponse(t.getId(), t.getFromStatusId(), t.getToStatusId(), t.getName());
        }
    }

    public record AddHookRequest(@NotNull UUID transitionId, @NotNull HookKind kind,
                                 @NotBlank String hookType, Map<String, Object> config) {}

    public record AddFieldRequest(@NotBlank String name, @NotNull FieldType fieldType, List<String> options) {}
    public record FieldResponse(UUID id, String name, FieldType fieldType, Object options) {
        public static FieldResponse from(CustomFieldDefinition d) {
            return new FieldResponse(d.getId(), d.getName(), d.getFieldType(), d.getOptions());
        }
    }
}
