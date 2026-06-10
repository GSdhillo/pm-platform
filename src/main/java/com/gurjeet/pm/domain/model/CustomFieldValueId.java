package com.gurjeet.pm.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CustomFieldValueId implements Serializable {
    private UUID issueId;
    private UUID fieldId;

    public CustomFieldValueId() {}
    public CustomFieldValueId(UUID issueId, UUID fieldId) { this.issueId = issueId; this.fieldId = fieldId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomFieldValueId that)) return false;
        return Objects.equals(issueId, that.issueId) && Objects.equals(fieldId, that.fieldId);
    }
    @Override public int hashCode() { return Objects.hash(issueId, fieldId); }
}
