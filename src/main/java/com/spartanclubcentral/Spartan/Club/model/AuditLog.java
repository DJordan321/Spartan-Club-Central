package com.spartanclubcentral.Spartan.Club.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Domain model: one security event (SPR-08).
 *
 * Records who (account id), what (action) and when. It has no fields for
 * emails, passwords or messages, so that data cannot end up here by mistake.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "event_time", nullable = false, updatable = false)
    private LocalDateTime eventTime;

    @Column(name = "actor_type", nullable = false, length = 10)
    private String actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "success", nullable = false)
    private boolean success;

    protected AuditLog() {
        // for JPA
    }

    public AuditLog(String actorType, Long actorId, String action,
                    String targetType, Long targetId, boolean success) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.success = success;
    }

    @PrePersist
    void onCreate() {
        if (eventTime == null) {
            eventTime = LocalDateTime.now();
        }
    }

    public Long getLogId() { return logId; }
    public LocalDateTime getEventTime() { return eventTime; }
    public String getActorType() { return actorType; }
    public Long getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public boolean isSuccess() { return success; }
}
