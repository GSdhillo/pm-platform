package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {
    List<WorkflowStatus> findByProjectIdOrderByPosition(UUID projectId);
    long countByProjectId(UUID projectId);
}
