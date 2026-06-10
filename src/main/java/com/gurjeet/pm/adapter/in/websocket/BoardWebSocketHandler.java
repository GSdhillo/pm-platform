package com.gurjeet.pm.adapter.in.websocket;

import com.gurjeet.pm.adapter.out.persistence.DomainEventRepository;
import com.gurjeet.pm.application.OutboxDispatcher;
import com.gurjeet.pm.application.PresenceService;
import com.gurjeet.pm.common.security.AuthUser;
import com.gurjeet.pm.domain.model.DomainEvent;
import com.gurjeet.pm.domain.port.BoardBroadcastPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class BoardWebSocketHandler extends TextWebSocketHandler implements BoardBroadcastPort, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(BoardWebSocketHandler.class);

    private final Map<UUID, Set<WebSocketSession>> sessionsByProject = new ConcurrentHashMap<>();
    private final DomainEventRepository eventRepository;
    private final OutboxDispatcher outboxDispatcher;
    private final PresenceService presenceService;

    public BoardWebSocketHandler(DomainEventRepository eventRepository,
                                 OutboxDispatcher outboxDispatcher,
                                 PresenceService presenceService,
                                 MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.outboxDispatcher = outboxDispatcher;
        this.presenceService = presenceService;
        Gauge.builder("ws.connections", sessionsByProject,
                        map -> map.values().stream().mapToInt(Set::size).sum())
                .description("Open board WebSocket connections")
                .register(meterRegistry);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) throws Exception {
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(rawSession, 5000, 256 * 1024);
        UUID projectId = (UUID) session.getAttributes().get("projectId");
        AuthUser user = (AuthUser) session.getAttributes().get("user");
        UUID issueId = (UUID) session.getAttributes().get("issueId");
        long lastSeq = (long) session.getAttributes().get("lastSeq");

        sessionsByProject.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>()).add(session);
        presenceService.join(projectId, issueId, user.id());
        broadcast(projectId, presenceMessage("presence_joined", user));

        if (lastSeq >= 0) {
            List<DomainEvent> missed = eventRepository
                    .findByProjectIdAndProjectSeqGreaterThanOrderByProjectSeqAsc(projectId, lastSeq);
            for (DomainEvent event : missed) {
                session.sendMessage(new TextMessage(outboxDispatcher.toWsMessage(event)));
            }
            log.info("Replayed {} missed events to user {} (after seq {})", missed.size(), user.id(), lastSeq);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID projectId = (UUID) session.getAttributes().get("projectId");
        AuthUser user = (AuthUser) session.getAttributes().get("user");
        UUID issueId = (UUID) session.getAttributes().get("issueId");
        Set<WebSocketSession> sessions = sessionsByProject.get(projectId);
        if (sessions != null) sessions.removeIf(s -> s.getId().equals(session.getId()));
        if (user != null) {
            presenceService.leave(projectId, issueId, user.id());
            broadcast(projectId, presenceMessage("presence_left", user));
        }
    }

    @Override
    public void broadcast(UUID projectId, String jsonMessage) {
        Set<WebSocketSession> sessions = sessionsByProject.get(projectId);
        if (sessions == null) return;
        TextMessage message = new TextMessage(jsonMessage);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) session.sendMessage(message);
            } catch (Exception e) {
                log.warn("WS send failed, dropping session {}", session.getId());
                sessions.remove(session);
            }
        }
    }

    @Override
    public void destroy() {
        log.info("Draining WebSocket sessions for shutdown");
        for (Set<WebSocketSession> sessions : sessionsByProject.values()) {
            for (WebSocketSession session : sessions) {
                try { session.close(CloseStatus.GOING_AWAY); } catch (Exception ignored) { }
            }
        }
        sessionsByProject.clear();
    }

    private String presenceMessage(String type, AuthUser user) {
        return "{\"type\":\"" + type + "\",\"user\":{\"id\":\"" + user.id()
                + "\",\"name\":\"" + user.displayName().replace("\"", "") + "\"}}";
    }
}
