package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {
    Optional<Issue> findByIssueKey(String issueKey);
    List<Issue> findByProjectId(UUID projectId);
    List<Issue> findBySprintId(UUID sprintId);
    List<Issue> findByParentId(UUID parentId);

    @Query("select count(i) from Issue i where i.statusId = :statusId")
    long countByStatusId(@Param("statusId") UUID statusId);

    @Query("select coalesce(sum(i.storyPoints),0) from Issue i where i.sprintId = :sprintId and i.statusId in :statusIds")
    long sumPointsInStatuses(@Param("sprintId") UUID sprintId, @Param("statusIds") List<UUID> statusIds);
}
