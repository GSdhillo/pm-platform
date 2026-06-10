package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gurjeet.pm.adapter.out.persistence.IssueRepository;
import com.gurjeet.pm.adapter.out.persistence.UserRepository;
import com.gurjeet.pm.adapter.out.persistence.WorkflowStatusRepository;
import com.gurjeet.pm.domain.model.Issue;
import com.gurjeet.pm.domain.model.Role;
import com.gurjeet.pm.domain.model.UserEntity;
import com.gurjeet.pm.domain.model.WorkflowStatus;
import com.gurjeet.pm.domain.port.BoardCachePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private final IssueRepository issueRepository;
    private final WorkflowStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;
    private final BoardCachePort boardCache;
    private final ObjectMapper objectMapper;

    public BoardService(IssueRepository issueRepository, WorkflowStatusRepository statusRepository,
                        UserRepository userRepository, AccessService accessService,
                        BoardCachePort boardCache, ObjectMapper objectMapper) {
        this.issueRepository = issueRepository;
        this.statusRepository = statusRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.boardCache = boardCache;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String boardJson(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        Optional<String> cached = boardCache.get(projectId);
        if (cached.isPresent()) return cached.get();

        List<WorkflowStatus> statuses = statusRepository.findByProjectIdOrderByPosition(projectId);
        List<Issue> issues = issueRepository.findByProjectId(projectId);

        Set<UUID> userIds = new HashSet<>();
        for (Issue issue : issues) if (issue.getAssigneeId() != null) userIds.add(issue.getAssigneeId());
        Map<UUID, String> names = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getDisplayName));

        Map<UUID, List<Issue>> byStatus = issues.stream().collect(Collectors.groupingBy(Issue::getStatusId));

        List<Map<String, Object>> columns = new ArrayList<>();
        for (WorkflowStatus status : statuses) {
            List<Map<String, Object>> cards = new ArrayList<>();
            for (Issue issue : byStatus.getOrDefault(status.getId(), List.of())) {
                Map<String, Object> card = new LinkedHashMap<>();
                card.put("id", issue.getId().toString());
                card.put("key", issue.getIssueKey());
                card.put("title", issue.getTitle());
                card.put("type", issue.getType().name());
                card.put("priority", issue.getPriority().name());
                card.put("storyPoints", issue.getStoryPoints());
                card.put("version", issue.getVersion());
                card.put("labels", issue.getLabels());
                card.put("sprintId", issue.getSprintId() == null ? null : issue.getSprintId().toString());
                if (issue.getAssigneeId() != null) {
                    card.put("assignee", Map.of("id", issue.getAssigneeId().toString(),
                            "name", names.getOrDefault(issue.getAssigneeId(), "?")));
                }
                cards.add(card);
            }
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("statusId", status.getId().toString());
            column.put("name", status.getName());
            column.put("category", status.getCategory().name());
            column.put("wipLimit", status.getWipLimit());
            column.put("count", cards.size());
            column.put("issues", cards);
            columns.add(column);
        }
        Map<String, Object> board = new LinkedHashMap<>();
        board.put("projectId", projectId.toString());
        board.put("columns", columns);
        try {
            String json = objectMapper.writeValueAsString(board);
            boardCache.put(projectId, json);
            return json;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize board", e);
        }
    }
}
