package com.spartanclubcentral.Spartan.Club.model;

import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Domain model: an administrator account for account recovery and deactivations. */
@Entity
@Table(name = "administrator")
public class Administrator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "name", nullable = false, length = FieldLimits.PERSON_NAME)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = FieldLimits.EMAIL)
    private String email;

    @Column(name = "password_hash", nullable = false, length = FieldLimits.PASSWORD_HASH)
    private String passwordHash;

    public Long getAdminId() { return adminId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
