package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.adapter.in.rest.dto.ProjectDtos.*;
import com.gurjeet.pm.application.PresenceService;
import com.gurjeet.pm.application.ProjectService;
import com.gurjeet.pm.application.SprintService;
import com.gurjeet.pm.common.security.AuthUser;
import com.gurjeet.pm.domain.model.Sprint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final SprintService sprintService;
    private final PresenceService presenceService;

    public ProjectController(ProjectService projectService, SprintService sprintService,
                             PresenceService presenceService) {
        this.projectService = projectService;
        this.sprintService = sprintService;
        this.presenceService = presenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return ProjectResponse.from(projectService.create(request.key(), request.name(), request.description(), user.id()));
    }

    @GetMapping
    public List<ProjectResponse> listMine(@AuthenticationPrincipal AuthUser user) {
        return projectService.listMine(user.id()).stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return ProjectResponse.from(projectService.get(projectId, user.id()));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user,
                       HttpServletRequest http) {
        projectService.deleteProject(projectId, user.id(), http.getRemoteAddr());
    }

    @GetMapping("/{projectId}/members")
    public List<MemberResponse> members(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return projectService.members(projectId, user.id()).stream().map(MemberResponse::from).toList();
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@PathVariable UUID projectId, @Valid @RequestBody AddMemberRequest request,
                          @AuthenticationPrincipal AuthUser user, HttpServletRequest http) {
        projectService.addMember(projectId, user.id(), request.email(), request.role(), http.getRemoteAddr());
    }

    @PatchMapping("/{projectId}/members/{userId}")
    public void changeRole(@PathVariable UUID projectId, @PathVariable UUID userId,
                           @Valid @RequestBody ChangeRoleRequest request,
                           @AuthenticationPrincipal AuthUser user, HttpServletRequest http) {
        projectService.changeRole(projectId, user.id(), userId, request.role(), http.getRemoteAddr());
    }

    @GetMapping("/{projectId}/workflow/statuses")
    public List<StatusResponse> statuses(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return projectService.statuses(projectId, user.id()).stream().map(StatusResponse::from).toList();
    }

    @PostMapping("/{projectId}/workflow/statuses")
    @ResponseStatus(HttpStatus.CREATED)
    public StatusResponse addStatus(@PathVariable UUID projectId, @Valid @RequestBody AddStatusRequest request,
                                    @AuthenticationPrincipal AuthUser user) {
        return StatusResponse.from(projectService.addStatus(projectId, user.id(),
                request.name(), request.category(), request.wipLimit()));
    }

    @GetMapping("/{projectId}/workflow/transitions")
    public List<TransitionResponse> transitions(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return projectService.transitions(projectId, user.id()).stream().map(TransitionResponse::from).toList();
    }

    @PostMapping("/{projectId}/workflow/transitions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransitionResponse addTransition(@PathVariable UUID projectId,
                                            @Valid @RequestBody AddTransitionRequest request,
                                            @AuthenticationPrincipal AuthUser user) {
        return TransitionResponse.from(projectService.addTransition(projectId, user.id(),
                request.fromStatusId(), request.toStatusId(), request.name()));
    }

    @PostMapping("/{projectId}/workflow/hooks")
    @ResponseStatus(HttpStatus.CREATED)
    public void addHook(@PathVariable UUID projectId, @Valid @RequestBody AddHookRequest request,
                        @AuthenticationPrincipal AuthUser user) {
        projectService.addHook(projectId, user.id(), request.transitionId(),
                request.kind(), request.hookType(), request.config() == null ? Map.of() : request.config());
    }

    @GetMapping("/{projectId}/fields")
    public List<FieldResponse> fields(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return projectService.fields(projectId, user.id()).stream().map(FieldResponse::from).toList();
    }

    @PostMapping("/{projectId}/fields")
    @ResponseStatus(HttpStatus.CREATED)
    public FieldResponse addField(@PathVariable UUID projectId, @Valid @RequestBody AddFieldRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return FieldResponse.from(projectService.addField(projectId, user.id(),
                request.name(), request.fieldType(), request.options()));
    }

    @GetMapping("/{projectId}/velocity")
    public List<Map<String, Object>> velocity(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return sprintService.velocity(projectId, user.id()).stream()
                .map(this::velocityEntry).toList();
    }

    private Map<String, Object> velocityEntry(Sprint sprint) {
        Map<String, Object> entry = new java.util.LinkedHashMap<>();
        entry.put("sprintId", sprint.getId().toString());
        entry.put("name", sprint.getName());
        entry.put("completedPoints", sprint.getCompletedPoints());
        entry.put("completedAt", sprint.getCompletedAt());
        return entry;
    }

    @GetMapping("/{projectId}/presence")
    public List<Map<String, String>> presence(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        projectService.get(projectId, user.id());
        return presenceService.viewers(projectId);
    }
}
