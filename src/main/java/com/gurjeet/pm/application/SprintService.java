package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.*;
import com.gurjeet.pm.common.error.BadRequestException;
import com.gurjeet.pm.common.error.ConflictException;
import com.gurjeet.pm.common.error.NotFoundException;
import com.gurjeet.pm.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SprintService {
    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final WorkflowStatusRepository statusRepository;
    private final ProjectRepository projectRepository;
    private final AccessService accessService;
    private final EventRecorder eventRecorder;

    public SprintService(SprintRepository sprintRepository, IssueRepository issueRepository,
                         WorkflowStatusRepository statusRepository, ProjectRepository projectRepository,
                         AccessService accessService, EventRecorder eventRecorder) {
        this.sprintRepository = sprintRepository;
        this.issueRepository = issueRepository;
        this.statusRepository = statusRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.eventRecorder = eventRecorder;
    }

    public record CompletionPreview(UUID sprintId, String sprintName,
                                    long completedCount, long completedPoints,
                                    List<Issue> incompleteIssues) {}

    public record CompletionResult(UUID sprintId, long completedPoints,
                                   List<String> carriedOver, List<String> movedToBacklog) {}

    @Transactional
    public Sprint create(UUID projectId, UUID actorId, String name, String goal, LocalDate start, LocalDate end) {
        accessService.requireRole(projectId, actorId, Role.PROJECT_LEAD);
        Sprint sprint = new Sprint(projectId, name, goal, start, end);
        sprintRepository.save(sprint);
        eventRecorder.record(projectId, "SPRINT_CREATED", "SPRINT", sprint.getId(), actorId,
                Map.of("name", name));
        return sprint;
    }

    @Transactional(readOnly = true)
    public List<Sprint> list(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return sprintRepository.findByProjectIdOrderByCreatedAt(projectId);
    }

    @Transactional
    public Sprint start(UUID sprintId, UUID actorId, LocalDate startDate, LocalDate endDate) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
        accessService.requireRole(sprint.getProjectId(), actorId, Role.PROJECT_LEAD);

        projectRepository.acquireTxLock(sprint.getProjectId().hashCode());

        if (sprint.getStatus() != SprintStatus.FUTURE) {
            throw new ConflictException("SPRINT_STATE", "Sprint is already " + sprint.getStatus(), null);
        }
        boolean anotherActive = !sprintRepository
                .findByProjectIdAndStatus(sprint.getProjectId(), SprintStatus.ACTIVE).isEmpty();
        if (anotherActive) {
            throw new ConflictException("ACTIVE_SPRINT_EXISTS",
                    "Another sprint is already active in this project. Complete it first.", null);
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        if (startDate != null) sprint.setStartDate(startDate);
        if (endDate != null) sprint.setEndDate(endDate);

        eventRecorder.record(sprint.getProjectId(), "SPRINT_STARTED", "SPRINT", sprint.getId(), actorId,
                Map.of("name", sprint.getName()));
        return sprint;
    }

    @Transactional(readOnly = true)
    public CompletionPreview completionPreview(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
        accessService.requireRole(sprint.getProjectId(), userId, Role.VIEWER);

        List<UUID> doneStatusIds = doneStatusIds(sprint.getProjectId());
        List<Issue> inSprint = issueRepository.findBySprintId(sprintId);
        List<Issue> incomplete = inSprint.stream()
                .filter(issue -> !doneStatusIds.contains(issue.getStatusId()))
                .collect(Collectors.toList());
        long completedCount = inSprint.size() - incomplete.size();
        long completedPoints = issueRepository.sumPointsInStatuses(sprintId, doneStatusIds);
        return new CompletionPreview(sprintId, sprint.getName(), completedCount, completedPoints, incomplete);
    }

    @Transactional
    public CompletionResult complete(UUID sprintId, UUID actorId,
                                     List<UUID> carryOverIssueIds, UUID targetSprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
        accessService.requireRole(sprint.getProjectId(), actorId, Role.PROJECT_LEAD);

        projectRepository.acquireTxLock(sprint.getProjectId().hashCode());

        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ConflictException("SPRINT_STATE", "Only an ACTIVE sprint can be completed (this one is "
                    + sprint.getStatus() + ")", null);
        }
        Sprint target = null;
        if (targetSprintId != null) {
            target = sprintRepository.findById(targetSprintId)
                    .orElseThrow(() -> new NotFoundException("Target sprint not found"));
            if (!target.getProjectId().equals(sprint.getProjectId()))
                throw new BadRequestException("Target sprint belongs to a different project");
            if (target.getStatus() != SprintStatus.FUTURE)
                throw new BadRequestException("Carry-over target must be a FUTURE sprint");
        }

        List<UUID> doneStatusIds = doneStatusIds(sprint.getProjectId());
        long completedPoints = issueRepository.sumPointsInStatuses(sprintId, doneStatusIds);

        Set<UUID> carryOver = carryOverIssueIds == null ? Set.of() : new HashSet<>(carryOverIssueIds);
        List<String> carried = new ArrayList<>();
        List<String> backlogged = new ArrayList<>();
        for (Issue issue : issueRepository.findBySprintId(sprintId)) {
            if (doneStatusIds.contains(issue.getStatusId())) continue;
            if (carryOver.contains(issue.getId()) && target != null) {
                issue.setSprintId(target.getId());
                carried.add(issue.getIssueKey());
            } else {
                issue.setSprintId(null);
                backlogged.add(issue.getIssueKey());
            }
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(Instant.now());
        sprint.setCompletedPoints((int) completedPoints);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", sprint.getName());
        payload.put("completedPoints", completedPoints);
        payload.put("carriedOver", carried);
        payload.put("movedToBacklog", backlogged);
        eventRecorder.record(sprint.getProjectId(), "SPRINT_COMPLETED", "SPRINT", sprint.getId(), actorId, payload);

        return new CompletionResult(sprintId, completedPoints, carried, backlogged);
    }

    @Transactional(readOnly = true)
    public List<Sprint> velocity(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return sprintRepository.findByProjectIdAndStatusOrderByCompletedAt(projectId, SprintStatus.COMPLETED);
    }

    private List<UUID> doneStatusIds(UUID projectId) {
        return statusRepository.findByProjectIdOrderByPosition(projectId).stream()
                .filter(s -> s.getCategory() == StatusCategory.DONE)
                .map(WorkflowStatus::getId)
                .collect(Collectors.toList());
    }
}
