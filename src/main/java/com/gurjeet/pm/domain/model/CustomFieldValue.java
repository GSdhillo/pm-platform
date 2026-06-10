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
@Table(name = "custom_field_values")
@IdClass(CustomFieldValueId.class)
@Getter @Setter @NoArgsConstructor
public class CustomFieldValue {
    @Id
    @Column(name = "issue_id")
    private UUID issueId;
    @Id
    @Column(name = "field_id")
    private UUID fieldId;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode value;

    public CustomFieldValue(UUID issueId, UUID fieldId, JsonNode value) {
        this.issueId = issueId;
        this.fieldId = fieldId;
        this.value = value;
    }
}
