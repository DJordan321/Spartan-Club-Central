package com.spartanclubcentral.Spartan.Club.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * Repository tests for the directory search (FR-01, FR-02, NFR-04).
 * Runs against in-memory H2 in MySQL mode; no MySQL install needed.
 */
@DataJpaTest
class ClubRepositoryTest {

    @Autowired
    private ClubRepository clubs;

    @Autowired
    private CategoryRepository categories;

    private Category academic;
    private Category sports;

    @BeforeEach
    void setUp() {
        academic = categories.save(new Category("Academic"));
        sports = categories.save(new Category("Sports"));
        Category cultural = categories.save(new Category("Cultural"));

        clubs.save(club("Chess Club", "Weekly games and tournaments.", academic, true));
        clubs.save(club("Engineering Builders Society",
                "Teams build robots for intercollegiate Robotics competitions.", academic, true));
        clubs.save(club("Soccer Club", "Pickup games every Saturday.", sports, true));
        clubs.save(club("Dance Collective", "Hip hop and step practice.", cultural, true));
        clubs.save(club("Retro Film Society", "Classic chess movies. Inactive.", cultural, false));
    }

    @Test
    @DisplayName("TC-01: keyword matches a club name")
    void keywordMatchesName() {
        assertThat(names(clubs.search("chess", null))).containsExactly("Chess Club");
    }

    @Test
    @DisplayName("TC-02: keyword matches a club description, case-insensitive")
    void keywordMatchesDescription() {
        assertThat(names(clubs.search("ROBOTICS", null)))
                .containsExactly("Engineering Builders Society");
    }

    @Test
    @DisplayName("TC-03: no match returns an empty list, not an error")
    void noMatch() {
        assertThat(clubs.search("zzzzqq", null)).isEmpty();
    }

    @Test
    @DisplayName("TC-04: category filter alone returns only that category")
    void categoryOnly() {
        assertThat(names(clubs.search(null, sports.getCategoryId()))).containsExactly("Soccer Club");
    }

    @Test
    @DisplayName("TC-05: keyword AND category are both applied")
    void keywordAndCategory() {
        // "club" matches Chess Club (Academic) and Soccer Club (Sports);
        // with Academic selected only Chess Club may appear.
        assertThat(names(clubs.search("club", academic.getCategoryId()))).containsExactly("Chess Club");
        assertThat(names(clubs.search("club", sports.getCategoryId()))).containsExactly("Soccer Club");
    }

    @Test
    @DisplayName("No keyword and no category lists all active clubs alphabetically")
    void everythingActiveSorted() {
        assertThat(names(clubs.search(null, null))).containsExactly(
                "Chess Club", "Dance Collective", "Engineering Builders Society", "Soccer Club");
    }

    @Test
    @DisplayName("Inactive clubs never appear in search or on the profile lookup (FR-12)")
    void inactiveHidden() {
        // "chess" is in the inactive club's description too, but only Chess Club shows.
        assertThat(names(clubs.search("chess", null))).containsExactly("Chess Club");

        Long chessId = idOf("Chess Club");
        Long retroId = idOf("Retro Film Society");
        assertThat(clubs.findByClubIdAndActiveTrue(chessId)).isPresent();
        assertThat(clubs.findByClubIdAndActiveTrue(retroId)).isEmpty();
    }

    @Test
    @DisplayName("SPR-05: SQL text in the keyword is treated as plain text")
    void sqlInjectionIsJustText() {
        assertThat(clubs.search("' OR 1=1 --", null)).isEmpty();
    }

    @Test
    @DisplayName("Saving a club sets last_updated automatically")
    void lastUpdatedSet() {
        Club saved = clubs.save(club("Math Circle", "Puzzles.", academic, true));
        assertThat(saved.getLastUpdated()).isNotNull();
    }

    private static Club club(String name, String description, Category category, boolean active) {
        Club c = new Club();
        c.setClubName(name);
        c.setDescription(description);
        c.setCategory(category);
        c.setContactEmail("contact@example.com");
        c.setActive(active);
        return c;
    }

    private Long idOf(String name) {
        return clubs.findAll().stream()
                .filter(c -> c.getClubName().equals(name))
                .findFirst().orElseThrow().getClubId();
    }

    private static List<String> names(List<Club> result) {
        return result.stream().map(Club::getClubName).toList();
    }
}
