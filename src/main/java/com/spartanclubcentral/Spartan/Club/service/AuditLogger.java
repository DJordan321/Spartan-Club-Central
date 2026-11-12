package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.model.AuditLog;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: records security events in the audit_log table (SPR-08).
 *
 * Only account ids, actions and targets are recorded: the AuditLog table has
 * no columns for emails, passwords or messages, so they can't leak into it.
 * Never throws: a logging problem must not break a login or registration.
 */
@Service
public class AuditLogger {

    // Actions used so far. Phase 5 and 6 add club and password-reset events.
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String REGISTERED = "REGISTERED";
    public static final String ADMIN_SEEDED = "ADMIN_SEEDED";
    public static final String DEMO_SEEDED = "DEMO_SEEDED";

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private final AuditLogRepository repository;

    public AuditLogger(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Runs in its own transaction so the event is kept even if the caller's work rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actorType, Long actorId, String action, String targetType, Long targetId, boolean success) {
        try {
            repository.save(new AuditLog(actorType, actorId, action, targetType, targetId, success));
        } catch (RuntimeException e) {
            log.warn("Could not write audit event {}: {}", action, e.getClass().getSimpleName());
        }
    }
}
