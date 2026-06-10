package com.gurjeet.pm.adapter.out.persistence;

import com.gurjeet.pm.domain.model.SecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {
}
