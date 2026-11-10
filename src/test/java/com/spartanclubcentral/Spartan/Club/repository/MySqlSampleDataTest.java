package com.spartanclubcentral.Spartan.Club.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.model.Club;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * OPT-IN check against your real local MySQL (skipped unless RUN_MYSQL_TESTS=true).
 *
 * Proves two things H2 cannot:
 *  1. database/schema.sql matches every entity (the app starts with ddl-auto=validate)
 *  2. database/sample-data.sql supports the demo searches (TC-02, TC-05, TC-06)
 *
 * Read-only: it never changes data. Run after loading schema.sql and sample-data.sql:
 *   RUN_MYSQL_TESTS=true DB_PASSWORD=... ./mvnw test -Dtest=MySqlSampleDataTest
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@TestPropertySource(properties = {
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/club_central}",
        "spring.datasource.username=${DB_USER:club_app}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MySqlSampleDataTest {

    private static final int ACADEMIC = 1;

    @Autowired
    private ClubRepository clubs;

    @Test
    void schemaMatchesEntitiesAndSampleDataSupportsTheDemo() {
        List<Club> all = clubs.search(null, null);
        assertThat(all).hasSizeGreaterThanOrEqualTo(30);
        assertThat(all).noneMatch(c -> c.getClubName().equals("Retro Film Society"));

        // TC-02: found through the description only
        assertThat(clubs.search("robotics", null))
                .extracting(Club::getClubName)
                .containsExactly("Engineering Builders Society");

        // TC-05: keyword + category together
        List<Club> academicClubs = clubs.search("club", ACADEMIC);
        assertThat(academicClubs).isNotEmpty();
        assertThat(academicClubs).allMatch(c -> c.getCategory().getCategoryId() == ACADEMIC);
        assertThat(clubs.search("club", null).size()).isGreaterThan(academicClubs.size());

        // TC-06: every active club has contact email, contact text or meeting time, and a social link
        assertThat(all).allSatisfy(c -> {
            assertThat(c.getContactEmail()).isNotBlank();
            assertThat(c.getMeetingTime()).isNotBlank();
            assertThat(c.getInstagramUrl() != null || c.getWebsiteUrl() != null
                    || c.getOtherSocialUrl() != null).isTrue();
        });
    }
}
