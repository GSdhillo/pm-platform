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
@Table(name = "security_audit_log")
@Getter @Setter @NoArgsConstructor
public class SecurityAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(nullable = false)
    private String action;
    private String target;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode details;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SecurityAudit(UUID actorId, String action, String target, JsonNode details, String ipAddress) {
        this.actorId = actorId;
        this.action = action;
        this.target = target;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = Instant.now();
    }
}
