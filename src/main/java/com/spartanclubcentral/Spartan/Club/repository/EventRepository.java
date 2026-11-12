package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.ClubEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for club events (FR-14). Ownership is checked in EventService first.
 * "Upcoming" means not over yet: it ends after now, or (no end time) starts after now.
 */
public interface EventRepository extends JpaRepository<ClubEvent, Long> {

    @Query("""
            SELECT e FROM ClubEvent e
            WHERE e.club.clubId = :clubId
              AND (e.endsAt >= :now OR (e.endsAt IS NULL AND e.startsAt >= :now))
            ORDER BY e.startsAt
            """)
    List<ClubEvent> findUpcoming(@Param("clubId") Long clubId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT e FROM ClubEvent e
            WHERE e.club.clubId = :clubId
              AND NOT (e.endsAt >= :now OR (e.endsAt IS NULL AND e.startsAt >= :now))
            ORDER BY e.startsAt DESC
            """)
    List<ClubEvent> findPast(@Param("clubId") Long clubId, @Param("now") LocalDateTime now);

    /** Upcoming events of ACTIVE clubs starting before :until, for the homepage. */
    @Query("""
            SELECT e FROM ClubEvent e JOIN FETCH e.club c
            WHERE c.active = true
              AND (e.endsAt >= :now OR (e.endsAt IS NULL AND e.startsAt >= :now))
              AND e.startsAt < :until
            ORDER BY e.startsAt
            """)
    List<ClubEvent> findUpcomingForActiveClubs(@Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

    long countByClub_ClubId(Long clubId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ClubEvent e WHERE e.club.clubId = :clubId")
    int deleteByClubId(@Param("clubId") Long clubId);
}
