package com.gurjeet.pm.adapter.in.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.gurjeet.pm.adapter.in.rest.dto.IssueDtos.*;
import com.gurjeet.pm.application.ActivityService;
import com.gurjeet.pm.application.IssueService;
import com.gurjeet.pm.application.IssueService.CreateCommand;
import com.gurjeet.pm.application.IssueService.UpdateCommand;
import com.gurjeet.pm.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class IssueController {
    private final IssueService issueService;
    private final ActivityService activityService;

    public IssueController(IssueService issueService, ActivityService activityService) {
        this.issueService = issueService;
        this.activityService = activityService;
    }

    @PostMapping("/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@PathVariable UUID projectId,
                                @Valid @RequestBody CreateIssueRequest request,
                                @AuthenticationPrincipal AuthUser user) {
        CreateCommand command = new CreateCommand(request.type(), request.title(), request.description(),
                request.priority(), request.assigneeId(), request.sprintId(), request.parentId(),
                request.storyPoints(), request.labels(), request.customFields());
        return IssueResponse.from(issueService.create(projectId, user.id(), command));
    }

    @GetMapping("/issues/{issueId}")
    public IssueResponse get(@PathVariable UUID issueId, @AuthenticationPrincipal AuthUser user) {
        return IssueResponse.from(issueService.get(issueId, user.id()));
    }

    @PatchMapping("/issues/{issueId}")
    public IssueResponse update(@PathVariable UUID issueId,
                                @Valid @RequestBody UpdateIssueRequest request,
                                @AuthenticationPrincipal AuthUser user) {
        UpdateCommand command = new UpdateCommand(
                request.expectedVersion(),
                request.title(), request.description(), request.priority(),
                nodeToUuid(request.assigneeId()), request.assigneeId() != null,
                nodeToUuid(request.sprintId()), request.sprintId() != null,
                nodeToUuid(request.parentId()), request.parentId() != null,
                nodeToInt(request.storyPoints()), request.storyPoints() != null,
                request.labels(), request.customFields());
        return IssueResponse.from(issueService.update(issueId, user.id(), command));
    }

    @PostMapping("/issues/{issueId}/transitions")
    public IssueResponse transition(@PathVariable UUID issueId,
                                    @Valid @RequestBody TransitionRequest request,
                                    @AuthenticationPrincipal AuthUser user) {
        return IssueResponse.from(issueService.transition(issueId, user.id(),
                request.toStatusId(), request.expectedVersion()));
    }

    @PostMapping("/issues/{issueId}/watchers/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void watch(@PathVariable UUID issueId, @AuthenticationPrincipal AuthUser user) {
        issueService.watch(issueId, user.id(), true);
    }

    @DeleteMapping("/issues/{issueId}/watchers/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unwatch(@PathVariable UUID issueId, @AuthenticationPrincipal AuthUser user) {
        issueService.watch(issueId, user.id(), false);
    }

    @GetMapping("/issues/{issueId}/history")
    public List<Map<String, Object>> history(@PathVariable UUID issueId,
                                             @RequestParam(defaultValue = "50") int limit,
                                             @AuthenticationPrincipal AuthUser user) {
        issueService.get(issueId, user.id());
        return activityService.issueHistory(issueId, limit).stream().map(event -> {
            Map<String, Object> entry = new java.util.LinkedHashMap<String, Object>();
            entry.put("id", event.getId());
            entry.put("type", event.getEventType());
            entry.put("actorId", event.getActorId());
            entry.put("at", event.getCreatedAt());
            entry.put("payload", event.getPayload());
            return entry;
        }).toList();
    }

    private UUID nodeToUuid(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return UUID.fromString(node.asText());
    }
    private Integer nodeToInt(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.asInt();
    }
}
