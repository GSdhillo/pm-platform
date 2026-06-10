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
@Table(name = "idempotency_keys")
@Getter @Setter @NoArgsConstructor
public class IdempotencyKey {
    @Id
    @Column(name = "key")
    private String key;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "request_hash", nullable = false)
    private String requestHash;
    @Column(name = "response_status")
    private Integer responseStatus;
    @Type(JsonType.class)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private JsonNode responseBody;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyKey(String key, UUID userId, String requestHash) {
        this.key = key;
        this.userId = userId;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }
}
