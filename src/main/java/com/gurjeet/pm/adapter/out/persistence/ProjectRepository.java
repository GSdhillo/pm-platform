package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByProjectKey(String projectKey);
    boolean existsByProjectKey(String projectKey);

    @Query(value = "SELECT next_issue_seq(:projectId)", nativeQuery = true)
    Long nextIssueSeq(@Param("projectId") UUID projectId);

    @Query(value = "SELECT next_event_seq(:projectId)", nativeQuery = true)
    Long nextEventSeq(@Param("projectId") UUID projectId);

    @Query(value = "SELECT acquire_tx_lock(:key)", nativeQuery = true)
    Boolean acquireTxLock(@Param("key") long key);
}
