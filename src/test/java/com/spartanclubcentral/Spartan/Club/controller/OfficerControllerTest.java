package com.spartanclubcentral.Spartan.Club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.OfficerRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Officers on club pages and in the leader dashboard (FR-13, SPR-03, NFR-02). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OfficerControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private LeaderRepository leaders;
    @Autowired private OfficerRepository officers;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private EntityManager em;

    private Club chess;
    private Club soccer;
    private ClubLeader leaderA;
    private ClubLeader leaderB;
    private ClubOfficer chessPresident;
    private ClubOfficer chessTreasurer;
    private ClubOfficer soccerCaptain;

    @BeforeEach
    void setUp() {
        Category academic = categories.save(new Category("Academic"));
        chess = clubs.save(club("Chess Club", academic));
        soccer = clubs.save(club("Soccer Club", academic));
        leaderA = leaders.save(leader("Alex Owner", "a@example.com", chess));
        leaderB = leaders.save(leader("Blair Other", "b@example.com", soccer));
        chessPresident = officers.save(new ClubOfficer(chess, "Alex Owner", "President", 1));
        chessTreasurer = officers.save(new ClubOfficer(chess, "Priya Patel", "Treasurer", 2));
        soccerCaptain = officers.save(new ClubOfficer(soccer, "Luis Moreno", "Captain", 1));
        officers.flush();
    }

    @Test
    @DisplayName("Public club page lists officers in order, with no emails")
    void publicPage() throws Exception {
        String html = mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Officers", "Alex Owner", "Priya Patel", "Treasurer").doesNotContain("a@example.com");
        assertThat(html.indexOf("Alex Owner")).isLessThan(html.indexOf("Priya Patel"));
    }

    @Test
    @DisplayName("Leader adds, edits, reorders and removes officers; the public page follows")
    void manageOfficers() throws Exception {
        mvc.perform(post(base(chess)).with(csrf()).with(user(me(leaderA))).param("name", "  Marcus Green ").param("role", "Events Chair"))
                .andExpect(redirectedUrl(base(chess) + "?added"));
        List<ClubOfficer> list = officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(chess.getClubId());
        assertThat(list).extracting(ClubOfficer::getName).containsExactly("Alex Owner", "Priya Patel", "Marcus Green");

        mvc.perform(post(base(chess) + "/" + chessTreasurer.getOfficerId() + "/move-up").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl(base(chess) + "?moved"));
        mvc.perform(post(base(chess) + "/" + chessTreasurer.getOfficerId()).with(csrf()).with(user(me(leaderA)))
                        .param("name", "Priya Patel").param("role", "Vice President"))
                .andExpect(redirectedUrl(base(chess) + "?saved"));
        em.flush(); em.clear();
        assertThat(officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(chess.getClubId()))
                .extracting(o -> o.getName() + "/" + o.getRole())
                .containsExactly("Priya Patel/Vice President", "Alex Owner/President", "Marcus Green/Events Chair");

        mvc.perform(get(base(chess) + "/" + chessPresident.getOfficerId() + "/remove").with(user(me(leaderA))))
                .andExpect(content().string(containsString("Remove <span>Alex Owner</span>?")));
        mvc.perform(post(base(chess) + "/" + chessPresident.getOfficerId() + "/remove").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl(base(chess) + "?removed"));
        em.flush(); em.clear();
        assertThat(officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(chess.getClubId()))
                .extracting(ClubOfficer::getDisplayOrder).containsExactly(1, 2);
        assertThat(auditLogs.findAll()).anyMatch(a -> a.getAction().equals(AuditLogger.OFFICERS_UPDATED));
    }

    @Test
    @DisplayName("Validation messages, typed values kept, nothing saved; 10-officer limit")
    void validationAndLimit() throws Exception {
        mvc.perform(post(base(chess)).with(csrf()).with(user(me(leaderA))).param("name", "Kim").param("role", ""))
                .andExpect(content().string(allOf(containsString("Enter their role"), containsString("value=\"Kim\""))));
        for (int i = 3; i <= 10; i++) {
            officers.save(new ClubOfficer(chess, "Officer " + i, "Member", i));
        }
        mvc.perform(post(base(chess)).with(csrf()).with(user(me(leaderA))).param("name", "Eleventh").param("role", "Member"))
                .andExpect(content().string(containsString("A club can list up to 10 officers.")));
        assertThat(officers.countByClub_ClubId(chess.getClubId())).isEqualTo(10);
    }

    @Test
    @DisplayName("SPR-03: Leader A can't touch Leader B's officers, even through their own club's address")
    void ownership() throws Exception {
        AccountPrincipal a = me(leaderA);
        String b = base(soccer);
        String cap = "/" + soccerCaptain.getOfficerId();
        long before = denials();
        mvc.perform(get(b).with(user(a))).andExpect(status().isForbidden());
        mvc.perform(post(b).with(csrf()).with(user(a)).param("name", "X").param("role", "Y")).andExpect(status().isForbidden());
        mvc.perform(get(b + cap + "/edit").with(user(a))).andExpect(status().isForbidden());
        mvc.perform(post(b + cap).with(csrf()).with(user(a)).param("name", "Hacked").param("role", "Y")).andExpect(status().isForbidden());
        mvc.perform(post(b + cap + "/move-down").with(csrf()).with(user(a))).andExpect(status().isForbidden());
        mvc.perform(post(b + cap + "/remove").with(csrf()).with(user(a))).andExpect(status().isForbidden());
        // Soccer's officer id through Chess Club's (own) address
        mvc.perform(post(base(chess) + cap).with(csrf()).with(user(a)).param("name", "Hacked").param("role", "Y"))
                .andExpect(status().isForbidden());
        mvc.perform(post(base(chess) + cap + "/remove").with(csrf()).with(user(a))).andExpect(status().isForbidden());
        em.flush(); em.clear();
        assertThat(officers.findById(soccerCaptain.getOfficerId()).orElseThrow().getName()).isEqualTo("Luis Moreno");
        assertThat(denials() - before).isEqualTo(8);
    }

    @Test
    @DisplayName("Creating a club lists the leader as first officer; deleting it removes its officers")
    void createAndDeleteClub() throws Exception {
        ClubLeader c = leaders.save(leader("Casey New", "c@example.com", null));
        c.setPosition("President");
        Category cat = chess.getCategory();
        mvc.perform(post("/dashboard/clubs").with(csrf()).with(user(me(c))).param("clubName", "Robotics Club")
                        .param("description", "Robots.").param("categoryId", cat.getCategoryId().toString())
                        .param("contactEmail", "robots@example.com"))
                .andExpect(redirectedUrl("/dashboard?created"));
        Club robotics = clubs.findByClubNameIgnoreCase("Robotics Club").orElseThrow();
        assertThat(officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(robotics.getClubId()))
                .extracting(o -> o.getName() + "/" + o.getRole()).containsExactly("Casey New/President");

        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/delete").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard?deleted"));
        em.flush(); em.clear();
        assertThat(officers.countByClub_ClubId(chess.getClubId())).isZero();
        assertThat(officers.countByClub_ClubId(soccer.getClubId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Hidden club: its officers aren't public either")
    void hiddenClub() throws Exception {
        chess.setActive(false);
        clubs.saveAndFlush(chess);
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Priya Patel"))));
    }

    private static String base(Club c) { return "/dashboard/clubs/" + c.getClubId() + "/officers"; }

    private static AccountPrincipal me(ClubLeader l) {
        return new AccountPrincipal(l.getLeaderId(), l.getEmail(), l.getName(), "x", Role.LEADER, true);
    }

    private long denials() {
        return auditLogs.findAll().stream().filter(l -> l.getAction().equals(AuditLogger.OWNERSHIP_DENIED)).count();
    }

    private static Club club(String name, Category category) {
        Club c = new Club();
        c.setClubName(name);
        c.setDescription("About " + name);
        c.setCategory(category);
        c.setContactEmail("contact@example.com");
        return c;
    }

    private static ClubLeader leader(String name, String email, Club club) {
        ClubLeader l = new ClubLeader();
        l.setName(name);
        l.setEmail(email);
        l.setPasswordHash("$2a$04$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234");
        l.setClub(club);
        return l;
    }
}
