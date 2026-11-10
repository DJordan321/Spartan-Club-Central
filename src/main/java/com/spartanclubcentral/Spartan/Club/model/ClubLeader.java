package com.spartanclubcentral.Spartan.Club.model;

import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Domain model: a club leader's account (FR-04, FR-08).
 *
 * club is null until the leader creates their club. passwordHash only ever
 * holds a BCrypt hash, never the typed password (NFR-01, SPR-02).
 */
@Entity
@Table(name = "club_leader")
public class ClubLeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leader_id")
    private Long leaderId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", unique = true)
    private Club club;

    @Column(name = "name", nullable = false, length = FieldLimits.PERSON_NAME)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = FieldLimits.EMAIL)
    private String email;

    @Column(name = "password_hash", nullable = false, length = FieldLimits.PASSWORD_HASH)
    private String passwordHash;

    @Column(name = "position", length = FieldLimits.POSITION)
    private String position;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** The owned club's id without loading the whole club, or null if none yet. */
    public Long getClubId() {
        return club == null ? null : club.getClubId();
    }

    public Long getLeaderId() { return leaderId; }
    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
