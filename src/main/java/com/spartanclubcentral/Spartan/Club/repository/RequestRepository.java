package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for join requests (FR-03).
 * Callers must check club ownership in ClubService BEFORE using the
 * club-based methods (SPR-04); this layer does not know who is logged in.
 */
public interface RequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByClub_ClubIdOrderByDateSubmittedDesc(Long clubId);

    long countByClub_ClubId(Long clubId);

    /** Removes a club's requests before the club itself is deleted (FR-12). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JoinRequest r WHERE r.club.clubId = :clubId")
    int deleteByClubId(@Param("clubId") Long clubId);
}
