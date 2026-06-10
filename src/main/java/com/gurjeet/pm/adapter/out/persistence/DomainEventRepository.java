package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.DomainEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DomainEventRepository extends JpaRepository<DomainEvent, Long> {

    List<DomainEvent> findTop100ByDispatchedAtIsNullOrderByIdAsc();

    List<DomainEvent> findByProjectIdAndProjectSeqGreaterThanOrderByProjectSeqAsc(UUID projectId, long afterSeq);

    @Query("""
        select e from DomainEvent e
        where e.projectId = :projectId
          and (:cursor is null or e.id < :cursor)
          and (:eventType is null or e.eventType = :eventType)
          and (:actorId is null or e.actorId = :actorId)
        order by e.id desc
        """)
    List<DomainEvent> feed(@Param("projectId") UUID projectId,
                           @Param("cursor") Long cursor,
                           @Param("eventType") String eventType,
                           @Param("actorId") UUID actorId,
                           Pageable pageable);

    List<DomainEvent> findByAggregateIdOrderByIdDesc(UUID aggregateId, Pageable pageable);
}
