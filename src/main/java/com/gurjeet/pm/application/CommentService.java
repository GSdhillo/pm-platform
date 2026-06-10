package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.CommentRepository;
import com.gurjeet.pm.adapter.out.persistence.IssueRepository;
import com.gurjeet.pm.common.error.BadRequestException;
import com.gurjeet.pm.common.error.NotFoundException;
import com.gurjeet.pm.domain.model.Comment;
import com.gurjeet.pm.domain.model.Issue;
import com.gurjeet.pm.domain.model.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final AccessService accessService;
    private final EventRecorder eventRecorder;

    public CommentService(CommentRepository commentRepository, IssueRepository issueRepository,
                          AccessService accessService, EventRecorder eventRecorder) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.accessService = accessService;
        this.eventRecorder = eventRecorder;
    }

    @Transactional
    public Comment add(UUID issueId, UUID actorId, String body, UUID parentCommentId, List<UUID> mentionUserIds) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        accessService.requireRole(issue.getProjectId(), actorId, Role.MEMBER);

        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));
            if (!parent.getIssueId().equals(issueId))
                throw new BadRequestException("Parent comment belongs to a different issue");
        }
        List<UUID> mentions = mentionUserIds == null ? List.of() : mentionUserIds;
        for (UUID mentioned : mentions) {
            accessService.requireRole(issue.getProjectId(), mentioned, Role.VIEWER);
        }

        Comment comment = new Comment(issueId, parentCommentId, actorId, body,
                mentions.stream().map(UUID::toString).collect(Collectors.toList()));
        commentRepository.save(comment);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issueKey", issue.getIssueKey());
        payload.put("commentId", comment.getId().toString());
        payload.put("mentions", comment.getMentions());
        payload.put("preview", body.length() > 120 ? body.substring(0, 120) + "..." : body);
        eventRecorder.record(issue.getProjectId(), "COMMENT_ADDED", "ISSUE", issueId, actorId, payload);
        return comment;
    }

    @Transactional(readOnly = true)
    public List<Comment> list(UUID issueId, UUID userId, int page, int size) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        accessService.requireRole(issue.getProjectId(), userId, Role.VIEWER);
        return commentRepository.findByIssueIdOrderByCreatedAt(issueId, PageRequest.of(page, Math.min(size, 100)));
    }
}
