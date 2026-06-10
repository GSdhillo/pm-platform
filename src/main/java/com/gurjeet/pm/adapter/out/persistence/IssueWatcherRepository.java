package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.IssueWatcher;
import com.gurjeet.pm.domain.model.IssueWatcherId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueWatcherRepository extends JpaRepository<IssueWatcher, IssueWatcherId> {
    List<IssueWatcher> findByIssueId(UUID issueId);
}
