package com.gurjeet.pm.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Entity
@Table(name = "transition_hooks")
@Getter @Setter @NoArgsConstructor
public class TransitionHook {
    @Id
    private UUID id;
    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HookKind kind;
    @Column(name = "hook_type", nullable = false)
    private String hookType;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode config;

    public TransitionHook(UUID transitionId, HookKind kind, String hookType, JsonNode config) {
        this.id = UUID.randomUUID();
        this.transitionId = transitionId;
        this.kind = kind;
        this.hookType = hookType;
        this.config = config;
    }
}
