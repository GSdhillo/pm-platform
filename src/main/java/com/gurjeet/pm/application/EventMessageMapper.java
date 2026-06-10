package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gurjeet.pm.domain.model.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventMessageMapper {
    private static final Map<String, String> WS_EVENT_NAMES = Map.of(
            "ISSUE_CREATED", "issue_created",
            "ISSUE_UPDATED", "issue_updated",
            "STATUS_CHANGED", "issue_moved",
            "COMMENT_ADDED", "comment_added",
            "SPRINT_CREATED", "sprint_updated",
            "SPRINT_STARTED", "sprint_updated",
            "SPRINT_COMPLETED", "sprint_updated"
    );

    private final ObjectMapper objectMapper;

    public EventMessageMapper(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public String toWsMessage(DomainEvent event) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("type", WS_EVENT_NAMES.getOrDefault(event.getEventType(),
                event.getEventType().toLowerCase()));
        message.put("seq", event.getProjectSeq());
        message.put("aggregateId", event.getAggregateId().toString());
        if (event.getActorId() != null) message.put("actorId", event.getActorId().toString());
        message.put("at", event.getCreatedAt().toString());
        message.set("payload", event.getPayload());
        return message.toString();
    }
}
