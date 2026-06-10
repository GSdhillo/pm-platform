package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.TransitionHook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransitionHookRepository extends JpaRepository<TransitionHook, UUID> {
    List<TransitionHook> findByTransitionId(UUID transitionId);
}
