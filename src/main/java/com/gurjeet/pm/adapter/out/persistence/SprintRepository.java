package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.Sprint;
import com.gurjeet.pm.domain.model.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByProjectIdOrderByCreatedAt(UUID projectId);
    List<Sprint> findByProjectIdAndStatus(UUID projectId, SprintStatus status);
    List<Sprint> findByProjectIdAndStatusOrderByCompletedAt(UUID projectId, SprintStatus status);
}
