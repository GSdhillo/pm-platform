package com.gurjeet.pm.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domain_events")
@Getter @Setter @NoArgsConstructor
public class DomainEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "project_seq", nullable = false)
    private long projectSeq;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(name = "actor_id")
    private UUID actorId;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    public DomainEvent(UUID projectId, long projectSeq, String eventType, String aggregateType,
                       UUID aggregateId, UUID actorId, JsonNode payload) {
        this.projectId = projectId;
        this.projectSeq = projectSeq;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.actorId = actorId;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
}
