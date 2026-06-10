package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.adapter.in.rest.dto.IssueDtos.IssueResponse;
import com.gurjeet.pm.adapter.in.rest.dto.SprintDtos.*;
import com.gurjeet.pm.application.SprintService;
import com.gurjeet.pm.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SprintController {
    private final SprintService sprintService;

    public SprintController(SprintService sprintService) { this.sprintService = sprintService; }

    @PostMapping("/projects/{projectId}/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse create(@PathVariable UUID projectId,
                                 @Valid @RequestBody CreateSprintRequest request,
                                 @AuthenticationPrincipal AuthUser user) {
        return SprintResponse.from(sprintService.create(projectId, user.id(),
                request.name(), request.goal(), request.startDate(), request.endDate()));
    }

    @GetMapping("/projects/{projectId}/sprints")
    public List<SprintResponse> list(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return sprintService.list(projectId, user.id()).stream().map(SprintResponse::from).toList();
    }

    @PostMapping("/sprints/{sprintId}/start")
    public SprintResponse start(@PathVariable UUID sprintId,
                                @RequestBody(required = false) StartSprintRequest request,
                                @AuthenticationPrincipal AuthUser user) {
        StartSprintRequest body = request == null ? new StartSprintRequest(null, null) : request;
        return SprintResponse.from(sprintService.start(sprintId, user.id(), body.startDate(), body.endDate()));
    }

    @GetMapping("/sprints/{sprintId}/completion-preview")
    public Map<String, Object> completionPreview(@PathVariable UUID sprintId,
                                                 @AuthenticationPrincipal AuthUser user) {
        var preview = sprintService.completionPreview(sprintId, user.id());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sprintId", preview.sprintId());
        response.put("sprintName", preview.sprintName());
        response.put("completedCount", preview.completedCount());
        response.put("completedPoints", preview.completedPoints());
        response.put("incompleteIssues", preview.incompleteIssues().stream().map(IssueResponse::from).toList());
        return response;
    }

    @PostMapping("/sprints/{sprintId}/complete")
    public Map<String, Object> complete(@PathVariable UUID sprintId,
                                        @RequestBody(required = false) CompleteSprintRequest request,
                                        @AuthenticationPrincipal AuthUser user) {
        CompleteSprintRequest body = request == null ? new CompleteSprintRequest(null, null) : request;
        var result = sprintService.complete(sprintId, user.id(), body.carryOverIssueIds(), body.targetSprintId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sprintId", result.sprintId());
        response.put("completedPoints", result.completedPoints());
        response.put("carriedOver", result.carriedOver());
        response.put("movedToBacklog", result.movedToBacklog());
        return response;
    }
}
