package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.Notification;
import com.gurjeet.pm.domain.model.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Notification> findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
