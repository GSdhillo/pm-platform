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
@Table(name = "custom_field_definitions")
@Getter @Setter @NoArgsConstructor
public class CustomFieldDefinition {
    @Id
    private UUID id;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode options;

    public CustomFieldDefinition(UUID projectId, String name, FieldType fieldType, JsonNode options) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.name = name;
        this.fieldType = fieldType;
        this.options = options;
    }
}
