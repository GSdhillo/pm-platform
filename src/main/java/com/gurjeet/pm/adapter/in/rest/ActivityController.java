package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.application.ActivityService;
import com.gurjeet.pm.common.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/activity")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) { this.activityService = activityService; }

    @GetMapping
    public Map<String, Object> feed(@PathVariable UUID projectId,
                                    @RequestParam(required = false) Long cursor,
                                    @RequestParam(required = false) String type,
                                    @RequestParam(required = false) UUID actorId,
                                    @RequestParam(defaultValue = "25") int limit,
                                    @AuthenticationPrincipal AuthUser user) {
        var page = activityService.feed(projectId, user.id(), cursor, type, actorId, limit);
        List<Map<String, Object>> events = page.events().stream().map(event -> {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("id", event.getId());
            entry.put("seq", event.getProjectSeq());
            entry.put("type", event.getEventType());
            entry.put("aggregateType", event.getAggregateType());
            entry.put("aggregateId", event.getAggregateId());
            entry.put("actorId", event.getActorId());
            entry.put("at", event.getCreatedAt());
            entry.put("payload", event.getPayload());
            return entry;
        }).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("events", events);
        response.put("nextCursor", page.nextCursor());
        return response;
    }
}
