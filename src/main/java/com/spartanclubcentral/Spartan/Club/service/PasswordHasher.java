package com.spartanclubcentral.Spartan.Club.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Service layer: the ONE place passwords are hashed and checked (NFR-01, SPR-02).
 *
 * Wraps the BCrypt PasswordEncoder defined in SecurityConfig, so registration,
 * login and (Phase 6) password reset all use exactly the same settings.
 * A BCrypt hash is salted and always 60 characters, like
 * $2a$12$... : the original password cannot be recovered from it.
 */
@Component
public class PasswordHasher {

    private final PasswordEncoder encoder;

    public PasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return rawPassword != null && hash != null && encoder.matches(rawPassword, hash);
    }
}
