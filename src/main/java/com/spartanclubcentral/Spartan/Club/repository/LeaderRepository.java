package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for club leader accounts (FR-04, FR-08). */
public interface LeaderRepository extends JpaRepository<ClubLeader, Long> {

    Optional<ClubLeader> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<ClubLeader> findByClub_ClubId(Long clubId);
}
