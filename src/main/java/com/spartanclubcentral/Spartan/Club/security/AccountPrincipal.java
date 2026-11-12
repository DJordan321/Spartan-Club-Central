package com.spartanclubcentral.Spartan.Club.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Security layer: the logged-in account (a club leader or an administrator).
 *
 * Built by AuthService at login. Controllers read the account id from here,
 * never from a form field or URL, so a user can't pretend to be someone else
 * (used by the ownership checks in Phase 5, SPR-03/SPR-04).
 * The password hash is wiped from memory right after login (SPR-02).
 */
public class AccountPrincipal implements UserDetails, CredentialsContainer {

    /** The two kinds of account. Spring Security role names are ROLE_ + this. */
    public enum Role { LEADER, ADMIN }

    private final Long id;
    private final String email;
    private final String displayName;
    private final Role role;
    private final boolean enabled;
    private String passwordHash;

    public AccountPrincipal(Long id, String email, String displayName, String passwordHash, Role role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public boolean isLeader() { return role == Role.LEADER; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public String toString() {
        // Never print the hash or email in logs.
        return "AccountPrincipal[" + role + " #" + id + "]";
    }
}
