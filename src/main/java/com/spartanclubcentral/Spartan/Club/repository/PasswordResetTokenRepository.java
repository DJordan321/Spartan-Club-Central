package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for password reset tokens, looked up by hash only (FR-11). */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** A leader's links that haven't been used yet (so a new reset can cancel the old ones). */
    List<PasswordResetToken> findByLeader_LeaderIdAndUsedAtIsNull(Long leaderId);
}
