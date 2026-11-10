package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for join requests (FR-03).
 * Callers must check club ownership in RequestService BEFORE using
 * findByClub... (SPR-04); this layer does not know who is logged in.
 */
public interface RequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByClub_ClubIdOrderByDateSubmittedDesc(Long clubId);
}
