package com.gurjeet.pm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gurjeet.pm.adapter.out.persistence.DomainEventRepository;
import com.gurjeet.pm.adapter.out.persistence.ProjectRepository;
import com.gurjeet.pm.domain.model.DomainEvent;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class EventRecorder {
    private final DomainEventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public EventRecorder(DomainEventRepository eventRepository,
                         ProjectRepository projectRepository,
                         ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
    }

    public DomainEvent record(UUID projectId, String eventType, String aggregateType,
                              UUID aggregateId, UUID actorId, Map<String, Object> payload) {
        long seq = projectRepository.nextEventSeq(projectId);
        DomainEvent event = new DomainEvent(projectId, seq, eventType, aggregateType,
                aggregateId, actorId, objectMapper.valueToTree(payload));
        return eventRepository.save(event);
    }
}
