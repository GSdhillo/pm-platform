package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.application.BoardService;
import com.gurjeet.pm.common.security.AuthUser;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) { this.boardService = boardService; }

    @GetMapping(value = "/projects/{projectId}/board", produces = MediaType.APPLICATION_JSON_VALUE)
    public String board(@PathVariable UUID projectId, @AuthenticationPrincipal AuthUser user) {
        return boardService.boardJson(projectId, user.id());
    }
}
