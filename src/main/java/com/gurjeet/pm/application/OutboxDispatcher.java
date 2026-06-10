package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gurjeet.pm.adapter.out.persistence.DomainEventRepository;
import com.gurjeet.pm.domain.model.DomainEvent;
import com.gurjeet.pm.domain.port.BoardBroadcastPort;
import com.gurjeet.pm.domain.port.BoardCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final Map<String, String> WS_EVENT_NAMES = Map.of(
            "ISSUE_CREATED", "issue_created",
            "ISSUE_UPDATED", "issue_updated",
            "STATUS_CHANGED", "issue_moved",
            "COMMENT_ADDED", "comment_added",
            "SPRINT_CREATED", "sprint_updated",
            "SPRINT_STARTED", "sprint_updated",
            "SPRINT_COMPLETED", "sprint_updated"
    );

    private final DomainEventRepository eventRepository;
    private final BoardBroadcastPort broadcastPort;
    private final BoardCachePort boardCache;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(DomainEventRepository eventRepository, BoardBroadcastPort broadcastPort,
                            BoardCachePort boardCache, NotificationService notificationService,
                            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.broadcastPort = broadcastPort;
        this.boardCache = boardCache;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:200}")
    @Transactional
    public void dispatch() {
        List<DomainEvent> batch = eventRepository.findTop100ByDispatchedAtIsNullOrderByIdAsc();
        for (DomainEvent event : batch) {
            try {
                broadcastPort.broadcast(event.getProjectId(), toWsMessage(event));
                notificationService.fanout(event);
                boardCache.evict(event.getProjectId());
                event.setDispatchedAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to dispatch event {} ({})", event.getId(), event.getEventType(), e);
                return;
            }
        }
    }

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
