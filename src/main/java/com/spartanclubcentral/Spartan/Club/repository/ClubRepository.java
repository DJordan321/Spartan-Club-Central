package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.Club;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for clubs. The only code that queries the club table.
 * All queries use bound parameters, never string-built SQL (SPR-05).
 */
public interface ClubRepository extends JpaRepository<Club, Long> {

    /**
     * Directory search (FR-01, FR-02): ONE query for every combination.
     *
     * - keyword null: no text filter; otherwise it must appear in the name OR
     *   the description (fixes TC-02)
     * - categoryId null: all categories; otherwise it is ALWAYS applied, even
     *   with a keyword (fixes TC-05). The parentheses around the two LIKEs
     *   matter: without them AND binds tighter and skips the category check.
     * - inactive clubs never appear
     *
     * Trade-off: LIKE '%word%' scans every active club, which takes
     * milliseconds for ~200 rows; FULLTEXT would skip short words like "AI".
     */
    @Query("""
            SELECT c FROM Club c
            WHERE c.active = true
              AND (:kw IS NULL
                   OR LOWER(c.clubName)    LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :kw, '%')))
              AND (:categoryId IS NULL OR c.category.categoryId = :categoryId)
            ORDER BY c.clubName
            """)
    List<Club> search(@Param("kw") String keyword, @Param("categoryId") Integer categoryId);

    /** Public profile lookup: inactive clubs behave as "not found" (FR-12). */
    Optional<Club> findByClubIdAndActiveTrue(Long clubId);

    boolean existsByClubNameIgnoreCase(String clubName);

    Optional<Club> findByClubNameIgnoreCase(String clubName);
}
