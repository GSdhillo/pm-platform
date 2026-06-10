package com.gurjeet.pm.adapter.in.rest.dto;

import com.gurjeet.pm.domain.model.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommentDtos {
    private CommentDtos() {}

    public record AddCommentRequest(@NotBlank @Size(max = 10000) String body,
                                    UUID parentCommentId,
                                    List<UUID> mentionUserIds) {}

    public record CommentResponse(UUID id, UUID issueId, UUID parentCommentId, UUID authorId,
                                  String body, List<String> mentions, Instant createdAt) {
        public static CommentResponse from(Comment c) {
            return new CommentResponse(c.getId(), c.getIssueId(), c.getParentCommentId(), c.getAuthorId(),
                    c.getBody(), c.getMentions(), c.getCreatedAt());
        }
    }
}
