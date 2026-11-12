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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.spartanclubcentral.Spartan.Club.model.AuditLog;
import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The leader dashboard end to end: create, edit, hide/show, delete, request
 * inbox, and above all the ownership rule (FR-08, FR-09, FR-12, NFR-02,
 * SPR-03, SPR-04). H2 database, no MySQL needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private LeaderRepository leaders;
    @Autowired private RequestRepository requests;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private EntityManager em;

    private Category academic;
    private Category sports;
    private ClubLeader leaderA;     // owns Chess Club
    private ClubLeader leaderB;     // owns Soccer Club
    private ClubLeader newLeader;   // no club yet
    private Club chess;
    private Club soccer;

    @BeforeEach
    void setUp() {
        academic = categories.save(new Category("Academic"));
        sports = categories.save(new Category("Sports"));
        chess = clubs.save(club("Chess Club", academic));
        soccer = clubs.save(club("Soccer Club", sports));
        leaderA = leaders.save(leader("Alex Owner", "a@example.com", chess));
        leaderB = leaders.save(leader("Blair Other", "b@example.com", soccer));
        newLeader = leaders.save(leader("Casey New", "c@example.com", null));
        requests.save(request(chess, "Chess Student", "chess.student@example.com", "Can I join?"));
        requests.save(request(soccer, "Soccer Student", "soccer.student@example.com", "When are tryouts?"));
        requests.flush();
    }

    // ---------- Dashboard overview ----------

    @Test
    @DisplayName("Leader with a club sees it, its status, and its request count")
    void overviewWithClub() throws Exception {
        mvc.perform(get("/dashboard").with(user(me(leaderA))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Chess Club"), containsString("Visible to students"),
                        containsString("/dashboard/clubs/" + chess.getClubId() + "/requests"),
                        containsString("<span class=\"count-pill\">1</span>"), not(containsString("Soccer Club")))));
    }

    @Test
    @DisplayName("Leader without a club is invited to create one")
    void overviewWithoutClub() throws Exception {
        mvc.perform(get("/dashboard").with(user(me(newLeader))))
                .andExpect(content().string(allOf(containsString("Create your club"), containsString("/dashboard/clubs/new"))));
    }

    // ---------- FR-08 create ----------

    @Test
    @DisplayName("TC-11: a leader without a club creates one; it's linked and public right away")
    void createClub() throws Exception {
        mvc.perform(post("/dashboard/clubs").with(csrf()).with(user(me(newLeader)))
                        .param("clubName", "  Robotics Club ").param("description", "We build robots.")
                        .param("categoryId", academic.getCategoryId().toString())
                        .param("contactEmail", "robotics@example.com").param("instagramUrl", "https://example.com/robots"))
                .andExpect(redirectedUrl("/dashboard?created"));

        Club created = clubs.findByClubNameIgnoreCase("Robotics Club").orElseThrow();
        assertThat(created.isActive()).isTrue();
        assertThat(created.getInstagramUrl()).isEqualTo("https://example.com/robots");
        assertThat(created.getWebsiteUrl()).isNull();
        assertThat(leaders.findById(newLeader.getLeaderId()).orElseThrow().getClubId()).isEqualTo(created.getClubId());
        assertThat(lastAction()).isEqualTo(AuditLogger.CLUB_CREATED);
        mvc.perform(get("/clubs").param("q", "robots")).andExpect(content().string(containsString("Robotics Club")));
    }

    @Test
    @DisplayName("Create form errors: messages shown, values kept, nothing saved; duplicate names refused")
    void createErrors() throws Exception {
        long before = clubs.count();
        mvc.perform(post("/dashboard/clubs").with(csrf()).with(user(me(newLeader)))
                        .param("clubName", "chess club").param("description", "")
                        .param("contactEmail", "x@").param("websiteUrl", "javascript:alert(1)"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/club-form"))
                .andExpect(content().string(allOf(
                        containsString("Another club already uses that name."),
                        containsString("Describe what the club does."),
                        containsString("Choose a category."),
                        containsString("Enter a valid email address"),
                        containsString("Enter a full web address starting with https://"),
                        containsString("value=\"chess club\""))));
        assertThat(clubs.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("A leader who already has a club can't create a second one")
    void oneClubPerLeader() throws Exception {
        mvc.perform(get("/dashboard/clubs/new").with(user(me(leaderA)))).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/dashboard/clubs").with(csrf()).with(user(me(leaderA)))
                        .param("clubName", "Second Club").param("description", "x")
                        .param("categoryId", academic.getCategoryId().toString()).param("contactEmail", "s@example.com"))
                .andExpect(status().isForbidden());
        assertThat(clubs.findByClubNameIgnoreCase("Second Club")).isEmpty();
    }

    // ---------- FR-09 edit ----------

    @Test
    @DisplayName("TC-12: the owner edits the club and students see the change")
    void editOwnClub() throws Exception {
        mvc.perform(get("/dashboard/clubs/" + chess.getClubId() + "/edit").with(user(me(leaderA))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("value=\"Chess Club\""), containsString("Save changes"))));

        mvc.perform(editRequest(leaderA, chess, "Chess Club", "Thursdays 7:00 PM, Library 300"))
                .andExpect(redirectedUrl("/dashboard?saved"));
        mvc.perform(get("/clubs/" + chess.getClubId()))
                .andExpect(content().string(containsString("Thursdays 7:00 PM, Library 300")));
        assertThat(lastAction()).isEqualTo(AuditLogger.CLUB_EDITED);
    }

    // ---------- FR-12 hide / show / delete ----------

    @Test
    @DisplayName("TC-13: hiding removes the club from students; showing brings it back")
    void hideAndShow() throws Exception {
        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/deactivate").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard?deactivated"));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isNotFound());
        mvc.perform(get("/clubs").param("q", "chess")).andExpect(content().string(containsString("No clubs found")));
        mvc.perform(get("/dashboard").with(user(me(leaderA))))
                .andExpect(content().string(allOf(containsString("Hidden from students"), containsString("Show club to students"))));

        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/activate").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard?activated"));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-14: delete asks first, then removes the club and its requests; the leader can start over")
    void deleteClub() throws Exception {
        mvc.perform(get("/dashboard/clubs/" + chess.getClubId() + "/delete").with(user(me(leaderA))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("This can't be undone."), containsString("1 join request"))));

        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/delete").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard?deleted"));
        em.flush();
        em.clear();
        assertThat(clubs.findById(chess.getClubId())).isEmpty();
        assertThat(requests.countByClub_ClubId(chess.getClubId())).isZero();
        assertThat(requests.countByClub_ClubId(soccer.getClubId())).isEqualTo(1);   // other club untouched
        assertThat(leaders.findById(leaderA.getLeaderId()).orElseThrow().getClubId()).isNull();
        assertThat(lastAction()).isEqualTo(AuditLogger.CLUB_DELETED);
        mvc.perform(get("/dashboard").with(user(me(leaderA)))).andExpect(content().string(containsString("Create your club")));
    }

    // ---------- NFR-02 / SPR-04 request inbox ----------

    @Test
    @DisplayName("TC-20: a leader's inbox shows only their own club's requests")
    void inboxShowsOnlyOwnRequests() throws Exception {
        mvc.perform(get("/dashboard/clubs/" + chess.getClubId() + "/requests").with(user(me(leaderA))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Chess Student"), containsString("chess.student@example.com"),
                        containsString("Can I join?"), not(containsString("soccer.student@example.com")))));
    }

    // ---------- SPR-03 / TC-19: never another leader's club ----------

    @Test
    @DisplayName("TC-19/TC-20: Leader A gets 403 for every action on Leader B's club, nothing changes, each attempt is logged")
    void cannotTouchAnotherLeadersClub() throws Exception {
        long id = soccer.getClubId();
        long deniedBefore = denials();
        AccountPrincipal a = me(leaderA);

        mvc.perform(get("/dashboard/clubs/" + id + "/edit").with(user(a))).andExpect(status().isForbidden());
        mvc.perform(editRequest(leaderA, soccer, "Hacked Club", "never")).andExpect(status().isForbidden());
        mvc.perform(post("/dashboard/clubs/" + id + "/deactivate").with(csrf()).with(user(a))).andExpect(status().isForbidden());
        mvc.perform(post("/dashboard/clubs/" + id + "/activate").with(csrf()).with(user(a))).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard/clubs/" + id + "/delete").with(user(a))).andExpect(status().isForbidden());
        mvc.perform(post("/dashboard/clubs/" + id + "/delete").with(csrf()).with(user(a))).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard/clubs/" + id + "/requests").with(user(a)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(allOf(containsString("That request wasn"),
                        not(containsString("soccer.student@example.com")), not(containsString("When are tryouts?")))));

        em.flush();
        em.clear();
        Club unchanged = clubs.findById(id).orElseThrow();
        assertThat(unchanged.getClubName()).isEqualTo("Soccer Club");
        assertThat(unchanged.isActive()).isTrue();
        assertThat(requests.countByClub_ClubId(id)).isEqualTo(1);
        assertThat(denials() - deniedBefore).isEqualTo(7);
    }

    @Test
    @DisplayName("A leader without a club can't act on any club either")
    void noClubLeaderDenied() throws Exception {
        mvc.perform(get("/dashboard/clubs/" + chess.getClubId() + "/requests").with(user(me(newLeader))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unknown club id -> 404; dashboard changes without CSRF token -> 403; logged out -> login")
    void otherGuards() throws Exception {
        mvc.perform(get("/dashboard/clubs/999999/edit").with(user(me(leaderA)))).andExpect(status().isNotFound());
        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/deactivate").with(user(me(leaderA))))
                .andExpect(status().isForbidden());
        assertThat(clubs.findById(chess.getClubId()).orElseThrow().isActive()).isTrue();
        mvc.perform(get("/dashboard/clubs/" + chess.getClubId() + "/requests")).andExpect(redirectedUrl("/login"));
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder editRequest(ClubLeader who, Club club, String name, String meetingTime) {
        return post("/dashboard/clubs/" + club.getClubId()).with(csrf()).with(user(me(who)))
                .param("clubName", name).param("description", "Updated description.")
                .param("categoryId", club.getCategory().getCategoryId().toString())
                .param("meetingTime", meetingTime).param("contactEmail", "contact@example.com");
    }

    private static AccountPrincipal me(ClubLeader l) {
        return new AccountPrincipal(l.getLeaderId(), l.getEmail(), l.getName(), "x", Role.LEADER, true);
    }

    private String lastAction() {
        return auditLogs.findAll().stream().max((x, y) -> Long.compare(x.getLogId(), y.getLogId()))
                .map(AuditLog::getAction).orElse(null);
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

    private static JoinRequest request(Club club, String name, String email, String message) {
        JoinRequest r = new JoinRequest();
        r.setClub(club);
        r.setStudentName(name);
        r.setStudentEmail(email);
        r.setMessage(message);
        return r;
    }
}
