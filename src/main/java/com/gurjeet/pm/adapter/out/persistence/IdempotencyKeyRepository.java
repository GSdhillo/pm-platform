package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
