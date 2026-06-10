package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.gurjeet.pm.adapter.out.persistence.*;
import com.gurjeet.pm.common.error.BadRequestException;
import com.gurjeet.pm.common.error.ConflictException;
import com.gurjeet.pm.common.error.NotFoundException;
import com.gurjeet.pm.common.error.UnprocessableException;
import com.gurjeet.pm.domain.model.*;
import com.gurjeet.pm.domain.workflow.WorkflowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowStatusRepository statusRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final TransitionHookRepository hookRepository;
    private final SprintRepository sprintRepository;
    private final IssueWatcherRepository watcherRepository;
    private final CustomFieldDefinitionRepository fieldDefRepository;
    private final CustomFieldValueRepository fieldValueRepository;
    private final ProjectMemberRepository memberRepository;
    private final AccessService accessService;
    private final EventRecorder eventRecorder;

    public IssueService(IssueRepository issueRepository, ProjectRepository projectRepository,
                        WorkflowStatusRepository statusRepository, WorkflowTransitionRepository transitionRepository,
                        TransitionHookRepository hookRepository, SprintRepository sprintRepository,
                        IssueWatcherRepository watcherRepository, CustomFieldDefinitionRepository fieldDefRepository,
                        CustomFieldValueRepository fieldValueRepository, ProjectMemberRepository memberRepository,
                        AccessService accessService, EventRecorder eventRecorder) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.hookRepository = hookRepository;
        this.sprintRepository = sprintRepository;
        this.watcherRepository = watcherRepository;
        this.fieldDefRepository = fieldDefRepository;
        this.fieldValueRepository = fieldValueRepository;
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.eventRecorder = eventRecorder;
    }

    public record CreateCommand(IssueType type, String title, String description, Priority priority,
                                UUID assigneeId, UUID sprintId, UUID parentId, Integer storyPoints,
                                List<String> labels, Map<UUID, JsonNode> customFields) {}

    public record UpdateCommand(long expectedVersion, String title, String description, Priority priority,
                                UUID assigneeId, boolean assigneeSet,
                                UUID sprintId, boolean sprintSet,
                                UUID parentId, boolean parentSet,
                                Integer storyPoints, boolean storyPointsSet,
                                List<String> labels, Map<UUID, JsonNode> customFields) {}

    @Transactional
    public Issue create(UUID projectId, UUID actorId, CreateCommand cmd) {
        accessService.requireRole(projectId, actorId, Role.MEMBER);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        WorkflowStatus initialStatus = statusRepository.findByProjectIdOrderByPosition(projectId).stream()
                .findFirst().orElseThrow(() -> new BadRequestException("Project has no workflow statuses"));

        long seq = projectRepository.nextIssueSeq(projectId);
        String issueKey = project.getProjectKey() + "-" + seq;
        Issue issue = new Issue(projectId, issueKey, cmd.type(), cmd.title(), initialStatus.getId(), actorId);
        issue.setDescription(cmd.description());
        if (cmd.priority() != null) issue.setPriority(cmd.priority());
        if (cmd.labels() != null) issue.setLabels(cmd.labels());
        issue.setStoryPoints(cmd.storyPoints());

        if (cmd.assigneeId() != null) {
            accessService.requireRole(projectId, cmd.assigneeId(), Role.VIEWER);
            issue.setAssigneeId(cmd.assigneeId());
        }
        if (cmd.sprintId() != null) {
            validateSprint(projectId, cmd.sprintId());
            issue.setSprintId(cmd.sprintId());
        }
        if (cmd.parentId() != null) {
            issue.setParentId(validateParent(projectId, cmd.type(), cmd.parentId()).getId());
        }
        issueRepository.save(issue);
        applyCustomFields(projectId, issue.getId(), cmd.customFields());

        watcherRepository.save(new IssueWatcher(issue.getId(), actorId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issueKey", issueKey);
        payload.put("title", cmd.title());
        payload.put("type", cmd.type().name());
        if (cmd.assigneeId() != null) payload.put("assigneeId", cmd.assigneeId().toString());
        eventRecorder.record(projectId, "ISSUE_CREATED", "ISSUE", issue.getId(), actorId, payload);
        return issue;
    }

    @Transactional
    public Issue update(UUID issueId, UUID actorId, UpdateCommand cmd) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        accessService.requireRole(issue.getProjectId(), actorId, Role.MEMBER);

        if (issue.getVersion() != cmd.expectedVersion()) {
            throw ConflictException.versionConflict(issue.getVersion(),
                    Map.of("title", issue.getTitle(),
                           "priority", issue.getPriority().name(),
                           "assigneeId", issue.getAssigneeId() == null ? "" : issue.getAssigneeId().toString(),
                           "storyPoints", issue.getStoryPoints() == null ? "" : issue.getStoryPoints().toString()));
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        if (cmd.title() != null && !cmd.title().equals(issue.getTitle())) {
            changes.put("title", Map.of("from", issue.getTitle(), "to", cmd.title()));
            issue.setTitle(cmd.title());
        }
        if (cmd.description() != null) {
            issue.setDescription(cmd.description());
            changes.put("description", Map.of("to", "updated"));
        }
        if (cmd.priority() != null && cmd.priority() != issue.getPriority()) {
            changes.put("priority", Map.of("from", issue.getPriority().name(), "to", cmd.priority().name()));
            issue.setPriority(cmd.priority());
        }
        if (cmd.assigneeSet()) {
            if (cmd.assigneeId() != null) accessService.requireRole(issue.getProjectId(), cmd.assigneeId(), Role.VIEWER);
            changes.put("assignee", mapOfNullable(issue.getAssigneeId(), cmd.assigneeId()));
            issue.setAssigneeId(cmd.assigneeId());
        }
        if (cmd.sprintSet()) {
            if (cmd.sprintId() != null) validateSprint(issue.getProjectId(), cmd.sprintId());
            changes.put("sprint", mapOfNullable(issue.getSprintId(), cmd.sprintId()));
            issue.setSprintId(cmd.sprintId());
        }
        if (cmd.parentSet()) {
            if (cmd.parentId() != null) validateParent(issue.getProjectId(), issue.getType(), cmd.parentId());
            issue.setParentId(cmd.parentId());
            changes.put("parent", mapOfNullable(null, cmd.parentId()));
        }
        if (cmd.storyPointsSet()) {
            changes.put("storyPoints", Map.of(
                    "from", issue.getStoryPoints() == null ? "null" : issue.getStoryPoints().toString(),
                    "to", cmd.storyPoints() == null ? "null" : cmd.storyPoints().toString()));
            issue.setStoryPoints(cmd.storyPoints());
        }
        if (cmd.labels() != null) {
            issue.setLabels(cmd.labels());
            changes.put("labels", Map.of("to", cmd.labels()));
        }
        applyCustomFields(issue.getProjectId(), issue.getId(), cmd.customFields());

        if (!changes.isEmpty()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("issueKey", issue.getIssueKey());
            payload.put("changes", changes);
            eventRecorder.record(issue.getProjectId(), "ISSUE_UPDATED", "ISSUE", issue.getId(), actorId, payload);
        }
        return issue;
    }

    @Transactional
    public Issue transition(UUID issueId, UUID actorId, UUID toStatusId, long expectedVersion) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        accessService.requireRole(issue.getProjectId(), actorId, Role.MEMBER);

        if (issue.getVersion() != expectedVersion) {
            throw ConflictException.versionConflict(issue.getVersion(), Map.of("statusId", issue.getStatusId().toString()));
        }

        List<WorkflowStatus> statuses = statusRepository.findByProjectIdOrderByPosition(issue.getProjectId());
        Map<UUID, WorkflowStatus> statusById = statuses.stream()
                .collect(Collectors.toMap(WorkflowStatus::getId, Function.identity()));
        WorkflowStatus current = statusById.get(issue.getStatusId());
        WorkflowStatus target = Optional.ofNullable(statusById.get(toStatusId))
                .orElseThrow(() -> new NotFoundException("Target status not found in this project"));

        List<WorkflowTransition> transitions = transitionRepository.findByProjectId(issue.getProjectId());
        WorkflowTransition transition = WorkflowEngine.requireTransition(current, target, transitions, statusById);

        List<TransitionHook> hooks = hookRepository.findByTransitionId(transition.getId());
        WorkflowEngine.runValidators(issue, hooks);

        if (target.getWipLimit() != null) {
            projectRepository.acquireTxLock(target.getId().hashCode());
            long inColumn = issueRepository.countByStatusId(target.getId());
            if (inColumn >= target.getWipLimit()) {
                throw new ConflictException("WIP_LIMIT_EXCEEDED",
                        "Column \"" + target.getName() + "\" is at its WIP limit (" + target.getWipLimit() + ")",
                        Map.of("wipLimit", target.getWipLimit(), "current", inColumn));
            }
        }

        WorkflowEngine.applyActions(issue, hooks);
        issue.setStatusId(target.getId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issueKey", issue.getIssueKey());
        payload.put("from", current.getName());
        payload.put("to", target.getName());
        if (issue.getAssigneeId() != null) payload.put("assigneeId", issue.getAssigneeId().toString());
        eventRecorder.record(issue.getProjectId(), "STATUS_CHANGED", "ISSUE", issue.getId(), actorId, payload);
        return issue;
    }

    @Transactional(readOnly = true)
    public Issue get(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        accessService.requireRole(issue.getProjectId(), userId, Role.VIEWER);
        return issue;
    }

    @Transactional
    public void watch(UUID issueId, UUID userId, boolean watching) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        accessService.requireRole(issue.getProjectId(), userId, Role.VIEWER);
        IssueWatcherId id = new IssueWatcherId(issueId, userId);
        if (watching) watcherRepository.save(new IssueWatcher(issueId, userId));
        else watcherRepository.deleteById(id);
    }

    private void validateSprint(UUID projectId, UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
        if (!sprint.getProjectId().equals(projectId)) throw new BadRequestException("Sprint belongs to a different project");
        if (sprint.getStatus() == SprintStatus.COMPLETED) throw new BadRequestException("Cannot add issues to a completed sprint");
    }

    private Issue validateParent(UUID projectId, IssueType childType, UUID parentId) {
        Issue parent = issueRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent issue not found"));
        if (!parent.getProjectId().equals(projectId)) throw new BadRequestException("Parent issue belongs to a different project");
        boolean valid = switch (childType) {
            case EPIC -> false;
            case STORY, TASK, BUG -> parent.getType() == IssueType.EPIC;
            case SUBTASK -> parent.getType() == IssueType.STORY || parent.getType() == IssueType.TASK || parent.getType() == IssueType.BUG;
        };
        if (!valid) {
            throw new UnprocessableException("INVALID_HIERARCHY",
                    childType + " cannot have a parent of type " + parent.getType()
                            + ". Allowed: EPIC -> STORY/TASK/BUG -> SUBTASK.", null);
        }
        return parent;
    }

    private void applyCustomFields(UUID projectId, UUID issueId, Map<UUID, JsonNode> values) {
        if (values == null || values.isEmpty()) return;
        Map<UUID, CustomFieldDefinition> defs = fieldDefRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(CustomFieldDefinition::getId, Function.identity()));
        for (Map.Entry<UUID, JsonNode> entry : values.entrySet()) {
            CustomFieldDefinition def = defs.get(entry.getKey());
            if (def == null) throw new BadRequestException("Unknown custom field: " + entry.getKey());
            JsonNode value = entry.getValue();
            switch (def.getFieldType()) {
                case NUMBER -> { if (!value.isNumber()) throw new BadRequestException("Field \"" + def.getName() + "\" expects a number"); }
                case TEXT -> { if (!value.isTextual()) throw new BadRequestException("Field \"" + def.getName() + "\" expects text"); }
                case DATE -> {
                    try { LocalDate.parse(value.asText()); }
                    catch (Exception e) { throw new BadRequestException("Field \"" + def.getName() + "\" expects an ISO date (yyyy-MM-dd)"); }
                }
                case DROPDOWN -> {
                    boolean ok = false;
                    for (JsonNode option : def.getOptions()) if (option.asText().equals(value.asText())) ok = true;
                    if (!ok) throw new BadRequestException("Field \"" + def.getName() + "\" must be one of " + def.getOptions());
                }
            }
            fieldValueRepository.save(new CustomFieldValue(issueId, def.getId(), value));
        }
    }

    private Map<String, Object> mapOfNullable(UUID from, UUID to) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("from", from == null ? "null" : from.toString());
        map.put("to", to == null ? "null" : to.toString());
        return map;
    }
}
