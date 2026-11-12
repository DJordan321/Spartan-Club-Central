package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for club officers (FR-13). Ownership is checked in OfficerService first. */
public interface OfficerRepository extends JpaRepository<ClubOfficer, Long> {

    List<ClubOfficer> findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(Long clubId);

    long countByClub_ClubId(Long clubId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ClubOfficer o WHERE o.club.clubId = :clubId")
    int deleteByClubId(@Param("clubId") Long clubId);
}
