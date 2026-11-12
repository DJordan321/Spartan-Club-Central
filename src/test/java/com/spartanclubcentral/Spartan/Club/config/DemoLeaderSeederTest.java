package com.spartanclubcentral.Spartan.Club.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The demo leader exists and can log in only when the "demo" profile is on. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class DemoLeaderSeederTest {

    @Autowired private MockMvc mvc;
    @Autowired private DemoLeaderSeeder seeder;
    @Autowired private LeaderRepository leaders;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private PasswordHasher hasher;

    @Test
    void demoLeaderIsCreatedLinkedToChessClubAndCanLogIn() throws Exception {
        // Fresh H2 test database: remove the startup copy, add Chess Club, seed again.
        leaders.findByEmailIgnoreCase(DemoLeaderSeeder.EMAIL).ifPresent(leaders::delete);
        Club chess = new Club();
        chess.setClubName("Chess Club");
        chess.setDescription("Weekly games.");
        chess.setCategory(categories.save(new Category("Academic")));
        chess.setContactEmail("chess@example.com");
        chess = clubs.save(chess);

        assertThat(seeder.seed()).isTrue();
        ClubLeader demo = leaders.findByEmailIgnoreCase(DemoLeaderSeeder.EMAIL).orElseThrow();
        assertThat(demo.getClubId()).isEqualTo(chess.getClubId());
        assertThat(demo.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(DemoLeaderSeeder.PASSWORD);
        assertThat(hasher.matches(DemoLeaderSeeder.PASSWORD, demo.getPasswordHash())).isTrue();
        assertThat(seeder.seed()).as("running again doesn't create a duplicate").isFalse();

        mvc.perform(formLogin("/login").userParameter("email").user(DemoLeaderSeeder.EMAIL).password(DemoLeaderSeeder.PASSWORD))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withRoles("LEADER"));
    }
}
