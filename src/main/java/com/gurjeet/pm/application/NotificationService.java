package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gurjeet.pm.adapter.out.persistence.IssueWatcherRepository;
import com.gurjeet.pm.adapter.out.persistence.NotificationRepository;
import com.gurjeet.pm.common.error.NotFoundException;
import com.gurjeet.pm.domain.model.*;
import com.gurjeet.pm.domain.port.NotificationPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final IssueWatcherRepository watcherRepository;
    private final NotificationPort notificationPort;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               IssueWatcherRepository watcherRepository,
                               NotificationPort notificationPort,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.watcherRepository = watcherRepository;
        this.notificationPort = notificationPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void fanout(DomainEvent event) {
        Set<UUID> recipients = new LinkedHashSet<>();
        String notificationType;
        switch (event.getEventType()) {
            case "STATUS_CHANGED" -> {
                notificationType = "STATUS_CHANGED";
                addWatchers(event.getAggregateId(), recipients);
                addIfPresent(event.getPayload().path("assigneeId").asText(null), recipients);
            }
            case "ISSUE_UPDATED" -> {
                notificationType = "ASSIGNED";
                String newAssignee = event.getPayload().path("changes").path("assignee").path("to").asText(null);
                if (newAssignee == null || newAssignee.equals("null")) return;
                addIfPresent(newAssignee, recipients);
            }
            case "COMMENT_ADDED" -> {
                notificationType = "COMMENT_ADDED";
                addWatchers(event.getAggregateId(), recipients);
                for (var mention : event.getPayload().path("mentions")) {
                    addIfPresent(mention.asText(null), recipients);
                }
            }
            case "ISSUE_CREATED" -> {
                notificationType = "ASSIGNED";
                addIfPresent(event.getPayload().path("assigneeId").asText(null), recipients);
            }
            default -> { return; }
        }
        recipients.remove(event.getActorId());
        for (UUID recipient : recipients) {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("eventType", event.getEventType());
            payload.put("projectId", event.getProjectId().toString());
            payload.put("aggregateId", event.getAggregateId().toString());
            payload.set("event", event.getPayload());
            notificationRepository.save(new Notification(recipient, notificationType, payload));
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.delivery-interval-ms:2000}")
    @Transactional
    public void deliverPending() {
        List<Notification> pending = notificationRepository
                .findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING);
        for (Notification notification : pending) {
            try {
                notificationPort.deliver(notification);
                notification.setStatus(NotificationStatus.DELIVERED);
            } catch (CallNotPermittedException breakerOpen) {
                log.warn("Notification circuit breaker OPEN; {} notifications queued", pending.size());
                return;
            } catch (Exception failure) {
                log.warn("Notification delivery failed (will retry): {}", failure.getMessage());
                return;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> myNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, Math.min(size, 100)));
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUserId().equals(userId)) throw new NotFoundException("Notification not found");
        notification.setReadAt(Instant.now());
    }

    private void addWatchers(UUID issueId, Set<UUID> recipients) {
        for (IssueWatcher watcher : watcherRepository.findByIssueId(issueId)) {
            recipients.add(watcher.getUserId());
        }
    }
    private void addIfPresent(String userId, Set<UUID> recipients) {
        if (userId != null && !userId.isBlank() && !"null".equals(userId)) {
            try { recipients.add(UUID.fromString(userId)); } catch (IllegalArgumentException ignored) { }
        }
    }
}
