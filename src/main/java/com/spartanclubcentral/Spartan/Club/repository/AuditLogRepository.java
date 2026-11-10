package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for security events (SPR-08). */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop50ByOrderByEventTimeDesc();
}
