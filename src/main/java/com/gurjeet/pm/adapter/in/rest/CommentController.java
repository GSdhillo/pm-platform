package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.adapter.in.rest.dto.CommentDtos.*;
import com.gurjeet.pm.application.CommentService;
import com.gurjeet.pm.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues/{issueId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) { this.commentService = commentService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable UUID issueId,
                               @Valid @RequestBody AddCommentRequest request,
                               @AuthenticationPrincipal AuthUser user) {
        return CommentResponse.from(commentService.add(issueId, user.id(),
                request.body(), request.parentCommentId(), request.mentionUserIds()));
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID issueId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size,
                                      @AuthenticationPrincipal AuthUser user) {
        return commentService.list(issueId, user.id(), page, size).stream()
                .map(CommentResponse::from).toList();
    }
}
