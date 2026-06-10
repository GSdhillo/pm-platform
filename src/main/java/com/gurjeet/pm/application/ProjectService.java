package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gurjeet.pm.adapter.out.persistence.*;
import com.gurjeet.pm.common.error.BadRequestException;
import com.gurjeet.pm.common.error.NotFoundException;
import com.gurjeet.pm.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkflowStatusRepository statusRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final TransitionHookRepository hookRepository;
    private final CustomFieldDefinitionRepository fieldRepository;
    private final SecurityAuditRepository auditRepository;
    private final AccessService accessService;
    private final EventRecorder eventRecorder;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                          UserRepository userRepository, WorkflowStatusRepository statusRepository,
                          WorkflowTransitionRepository transitionRepository, TransitionHookRepository hookRepository,
                          CustomFieldDefinitionRepository fieldRepository, SecurityAuditRepository auditRepository,
                          AccessService accessService, EventRecorder eventRecorder, ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.hookRepository = hookRepository;
        this.fieldRepository = fieldRepository;
        this.auditRepository = auditRepository;
        this.accessService = accessService;
        this.eventRecorder = eventRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Project create(String key, String name, String description, UUID creatorId) {
        String normalizedKey = key.toUpperCase();
        if (projectRepository.existsByProjectKey(normalizedKey)) {
            throw new BadRequestException("Project key already in use: " + normalizedKey);
        }
        Project project = new Project(normalizedKey, name, description, creatorId);
        projectRepository.save(project);
        memberRepository.save(new ProjectMember(project.getId(), creatorId, Role.ADMIN));
        createDefaultWorkflow(project.getId());
        eventRecorder.record(project.getId(), "PROJECT_CREATED", "PROJECT", project.getId(), creatorId,
                Map.of("name", name, "key", normalizedKey));
        return project;
    }

    private void createDefaultWorkflow(UUID projectId) {
        WorkflowStatus todo = new WorkflowStatus(projectId, "To Do", StatusCategory.TODO, 0, null);
        WorkflowStatus inProgress = new WorkflowStatus(projectId, "In Progress", StatusCategory.IN_PROGRESS, 1, null);
        WorkflowStatus inReview = new WorkflowStatus(projectId, "In Review", StatusCategory.IN_PROGRESS, 2, null);
        WorkflowStatus done = new WorkflowStatus(projectId, "Done", StatusCategory.DONE, 3, null);
        statusRepository.saveAll(List.of(todo, inProgress, inReview, done));

        WorkflowTransition start = new WorkflowTransition(projectId, todo.getId(), inProgress.getId(), "Start work");
        WorkflowTransition review = new WorkflowTransition(projectId, inProgress.getId(), inReview.getId(), "Submit for review");
        WorkflowTransition approve = new WorkflowTransition(projectId, inReview.getId(), done.getId(), "Approve");
        WorkflowTransition reject = new WorkflowTransition(projectId, inReview.getId(), inProgress.getId(), "Request changes");
        transitionRepository.saveAll(List.of(start, review, approve, reject));

        hookRepository.save(new TransitionHook(start.getId(), HookKind.VALIDATOR,
                "REQUIRE_ASSIGNEE", objectMapper.createObjectNode()));
    }

    @Transactional(readOnly = true)
    public Project get(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    @Transactional(readOnly = true)
    public List<Project> listMine(UUID userId) {
        List<UUID> ids = accessService.visibleProjectIds(userId);
        return projectRepository.findAllById(ids);
    }

    @Transactional
    public void addMember(UUID projectId, UUID actorId, String email, Role role, String ip) {
        accessService.requireRole(projectId, actorId, Role.ADMIN);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No user with email " + email));
        memberRepository.save(new ProjectMember(projectId, user.getId(), role));
        auditRepository.save(new SecurityAudit(actorId, "MEMBER_ADDED", "project:" + projectId,
                objectMapper.valueToTree(Map.of("user", email, "role", role.name())), ip));
    }

    @Transactional
    public void changeRole(UUID projectId, UUID actorId, UUID targetUserId, Role role, String ip) {
        accessService.requireRole(projectId, actorId, Role.ADMIN);
        ProjectMember member = memberRepository.findById(new ProjectMemberId(projectId, targetUserId))
                .orElseThrow(() -> new NotFoundException("User is not a member of this project"));
        Role oldRole = member.getRole();
        member.setRole(role);
        auditRepository.save(new SecurityAudit(actorId, "ROLE_CHANGED", "project:" + projectId,
                objectMapper.valueToTree(Map.of("user", targetUserId.toString(), "from", oldRole.name(), "to", role.name())), ip));
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID actorId, String ip) {
        accessService.requireRole(projectId, actorId, Role.ADMIN);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        auditRepository.save(new SecurityAudit(actorId, "PROJECT_DELETED", "project:" + projectId,
                objectMapper.valueToTree(Map.of("key", project.getProjectKey(), "name", project.getName())), ip));
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> members(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return memberRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowStatus> statuses(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return statusRepository.findByProjectIdOrderByPosition(projectId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowTransition> transitions(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return transitionRepository.findByProjectId(projectId);
    }

    @Transactional
    public WorkflowStatus addStatus(UUID projectId, UUID userId, String name, StatusCategory category, Integer wipLimit) {
        accessService.requireRole(projectId, userId, Role.PROJECT_LEAD);
        int position = (int) statusRepository.countByProjectId(projectId);
        WorkflowStatus status = new WorkflowStatus(projectId, name, category, position, wipLimit);
        return statusRepository.save(status);
    }

    @Transactional
    public WorkflowTransition addTransition(UUID projectId, UUID userId, UUID fromStatusId, UUID toStatusId, String name) {
        accessService.requireRole(projectId, userId, Role.PROJECT_LEAD);
        return transitionRepository.save(new WorkflowTransition(projectId, fromStatusId, toStatusId, name));
    }

    @Transactional
    public TransitionHook addHook(UUID projectId, UUID userId, UUID transitionId, HookKind kind, String hookType, Map<String, Object> config) {
        accessService.requireRole(projectId, userId, Role.PROJECT_LEAD);
        return hookRepository.save(new TransitionHook(transitionId, kind, hookType, objectMapper.valueToTree(config)));
    }

    @Transactional
    public CustomFieldDefinition addField(UUID projectId, UUID userId, String name, FieldType type, List<String> options) {
        accessService.requireRole(projectId, userId, Role.PROJECT_LEAD);
        return fieldRepository.save(new CustomFieldDefinition(projectId, name, type,
                objectMapper.valueToTree(options == null ? List.of() : options)));
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinition> fields(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        return fieldRepository.findByProjectId(projectId);
    }
}
