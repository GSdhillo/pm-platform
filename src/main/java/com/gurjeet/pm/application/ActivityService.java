package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.DomainEventRepository;
import com.gurjeet.pm.domain.model.DomainEvent;
import com.gurjeet.pm.domain.model.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {
    private final DomainEventRepository eventRepository;
    private final AccessService accessService;

    public ActivityService(DomainEventRepository eventRepository, AccessService accessService) {
        this.eventRepository = eventRepository;
        this.accessService = accessService;
    }

    public record FeedPage(List<DomainEvent> events, Long nextCursor) {}

    @Transactional(readOnly = true)
    public FeedPage feed(UUID projectId, UUID userId, Long cursor, String eventType, UUID actorId, int limit) {
        accessService.requireRole(projectId, userId, Role.VIEWER);
        int pageSize = Math.min(Math.max(limit, 1), 100);
        List<DomainEvent> events = eventRepository.feed(projectId, cursor, eventType, actorId,
                PageRequest.of(0, pageSize + 1));
        Long next = null;
        if (events.size() > pageSize) {
            events = events.subList(0, pageSize);
            next = events.get(events.size() - 1).getId();
        }
        return new FeedPage(events, next);
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> issueHistory(UUID issueId, int limit) {
        return eventRepository.findByAggregateIdOrderByIdDesc(issueId, PageRequest.of(0, Math.min(limit, 100)));
    }
}
