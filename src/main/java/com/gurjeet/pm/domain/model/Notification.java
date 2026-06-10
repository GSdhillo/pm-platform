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
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String type;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Notification(UUID userId, String type, JsonNode payload) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
}
