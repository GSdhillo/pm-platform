package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.CustomFieldValue;
import com.gurjeet.pm.domain.model.CustomFieldValueId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, CustomFieldValueId> {
    List<CustomFieldValue> findByIssueId(UUID issueId);
}
