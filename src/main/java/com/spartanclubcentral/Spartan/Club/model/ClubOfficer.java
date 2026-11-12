package com.spartanclubcentral.Spartan.Club.model;

import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Domain model: one officer shown on a club's public page (FR-13).
 * Name and role only, by design: no email, phone or photo of the person (NFR-02).
 */
@Entity
@Table(name = "club_officer")
public class ClubOfficer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "officer_id")
    private Long officerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "name", nullable = false, length = FieldLimits.PERSON_NAME)
    private String name;

    @Column(name = "role", nullable = false, length = FieldLimits.OFFICER_ROLE)
    private String role;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected ClubOfficer() {
        // for JPA
    }

    public ClubOfficer(Club club, String name, String role, int displayOrder) {
        this.club = club;
        this.name = name;
        this.role = role;
        this.displayOrder = displayOrder;
    }

    /** The owning club's id without loading the whole club. */
    public Long getClubId() {
        return club == null ? null : club.getClubId();
    }

    public Long getOfficerId() { return officerId; }
    public Club getClub() { return club; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
