package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.application.NotificationService;
import com.gurjeet.pm.common.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Map<String, Object>> mine(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size,
                                          @AuthenticationPrincipal AuthUser user) {
        return notificationService.myNotifications(user.id(), page, size).stream().map(notification -> {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("id", notification.getId());
            entry.put("type", notification.getType());
            entry.put("status", notification.getStatus());
            entry.put("readAt", notification.getReadAt());
            entry.put("createdAt", notification.getCreatedAt());
            entry.put("payload", notification.getPayload());
            return entry;
        }).toList();
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID notificationId, @AuthenticationPrincipal AuthUser user) {
        notificationService.markRead(notificationId, user.id());
    }
}
