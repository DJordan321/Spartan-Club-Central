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
import com.spartanclubcentral.Spartan.Club.model.ClubEvent;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.EventRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Club events on club pages, the homepage and the leader dashboard (FR-14, SPR-03). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private LeaderRepository leaders;
    @Autowired private EventRepository events;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private EntityManager em;

    private Club chess;
    private Club soccer;
    private ClubLeader leaderA;
    private ClubEvent chessSoon;
    private ClubEvent chessLater;
    private ClubEvent chessPast;
    private ClubEvent soccerGame;

    @BeforeEach
    void setUp() {
        Category academic = categories.save(new Category("Academic"));
        chess = clubs.save(club("Chess Club", academic));
        soccer = clubs.save(club("Soccer Club", academic));
        leaderA = leaders.save(leader("a@example.com", chess));
        leaders.save(leader("b@example.com", soccer));
        LocalDateTime now = LocalDateTime.now();
        chessLater = events.save(event(chess, "Beginner night", now.plusDays(9)));
        chessSoon = events.save(event(chess, "Blitz tournament", now.plusDays(2)));
        chessPast = events.save(event(chess, "Old simul", now.minusDays(3)));
        soccerGame = events.save(event(soccer, "Pickup game", now.plusDays(1)));
        events.flush();
    }

    @Test
    @DisplayName("Club page shows upcoming events soonest first; past events never")
    void publicUpcoming() throws Exception {
        String html = mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Upcoming events", "Blitz tournament", "Beginner night", "<time datetime=").doesNotContain("Old simul");
        assertThat(html.indexOf("Blitz tournament")).isLessThan(html.indexOf("Beginner night"));
    }

    @Test
    @DisplayName("Homepage 'Coming up this week' lists the next week's events of active clubs only")
    void homepage() throws Exception {
        soccer.setActive(false);
        clubs.saveAndFlush(soccer);
        mvc.perform(get("/")).andExpect(content().string(allOf(containsString("Coming up this week"),
                containsString("Blitz tournament"), not(containsString("Beginner night")),   // 9 days away
                not(containsString("Pickup game")), not(containsString("Old simul")))));
        mvc.perform(get("/clubs/" + soccer.getClubId())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Leader posts, edits and deletes an event; validation keeps typed values")
    void manageEvents() throws Exception {
        String date = LocalDate.now().plusDays(5).toString();
        mvc.perform(eventPost(base(chess), leaderA, "Puzzle night", date, "18:00", "17:00"))
                .andExpect(content().string(allOf(containsString("The end time must be after the start time."),
                        containsString("value=\"Puzzle night\""))));
        mvc.perform(eventPost(base(chess), leaderA, "Puzzle night", date, "18:00", "19:30"))
                .andExpect(redirectedUrl(base(chess) + "?created"));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(content().string(containsString("Puzzle night")));

        mvc.perform(eventPost(base(chess) + "/" + chessSoon.getEventId(), leaderA, "Blitz tournament (moved)", date, "19:00", ""))
                .andExpect(redirectedUrl(base(chess) + "?saved"));
        mvc.perform(get(base(chess) + "/" + chessLater.getEventId() + "/delete").with(user(me(leaderA))))
                .andExpect(content().string(containsString("Delete <span>Beginner night</span>?")));
        mvc.perform(post(base(chess) + "/" + chessLater.getEventId() + "/delete").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl(base(chess) + "?deleted"));
        em.flush(); em.clear();
        assertThat(events.findById(chessSoon.getEventId()).orElseThrow().getTitle()).isEqualTo("Blitz tournament (moved)");
        assertThat(events.findById(chessLater.getEventId())).isEmpty();
        assertThat(auditLogs.findAll()).extracting(a -> a.getAction())
                .contains(AuditLogger.EVENT_CREATED, AuditLogger.EVENT_EDITED, AuditLogger.EVENT_DELETED);
    }

    @Test
    @DisplayName("Dashboard events page lists upcoming and past events")
    void dashboardLists() throws Exception {
        mvc.perform(get(base(chess)).with(user(me(leaderA)))).andExpect(content().string(allOf(
                containsString("Upcoming (<span>2</span>)"), containsString("Past (<span>1</span>)"), containsString("Old simul"))));
        mvc.perform(get("/dashboard").with(user(me(leaderA)))).andExpect(content().string(containsString("Blitz tournament")));
    }

    @Test
    @DisplayName("SPR-05: script text in an event is shown as text")
    void escaping() throws Exception {
        mvc.perform(eventPost(base(chess), leaderA, "<script>alert(1)</script>", LocalDate.now().plusDays(4).toString(), "18:00", ""))
                .andExpect(redirectedUrl(base(chess) + "?created"));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(content().string(allOf(
                not(containsString("<script>alert(1)</script>")), containsString("&lt;script&gt;alert(1)&lt;/script&gt;"))));
    }

    @Test
    @DisplayName("SPR-03: Leader A can't touch Soccer Club's events, even through Chess Club's address")
    void ownership() throws Exception {
        String b = base(soccer);
        String game = "/" + soccerGame.getEventId();
        String date = LocalDate.now().plusDays(5).toString();
        long before = denials();
        mvc.perform(get(b).with(user(me(leaderA)))).andExpect(status().isForbidden());
        mvc.perform(get(b + "/new").with(user(me(leaderA)))).andExpect(status().isForbidden());
        mvc.perform(eventPost(b, leaderA, "Spam", date, "18:00", "")).andExpect(status().isForbidden());
        mvc.perform(get(b + game + "/edit").with(user(me(leaderA)))).andExpect(status().isForbidden());
        mvc.perform(eventPost(b + game, leaderA, "Hacked", date, "18:00", "")).andExpect(status().isForbidden());
        mvc.perform(post(b + game + "/delete").with(csrf()).with(user(me(leaderA)))).andExpect(status().isForbidden());
        mvc.perform(eventPost(base(chess) + game, leaderA, "Hacked", date, "18:00", "")).andExpect(status().isForbidden());
        mvc.perform(post(base(chess) + game + "/delete").with(csrf()).with(user(me(leaderA)))).andExpect(status().isForbidden());
        em.flush(); em.clear();
        assertThat(events.findById(soccerGame.getEventId()).orElseThrow().getTitle()).isEqualTo("Pickup game");
        assertThat(events.countByClub_ClubId(soccer.getClubId())).isEqualTo(1);
        assertThat(denials() - before).isEqualTo(8);
    }

    @Test
    @DisplayName("Deleting a club deletes its events")
    void deleteClub() throws Exception {
        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/delete").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard?deleted"));
        em.flush(); em.clear();
        assertThat(events.countByClub_ClubId(chess.getClubId())).isZero();
        assertThat(events.countByClub_ClubId(soccer.getClubId())).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder eventPost(String url, ClubLeader who, String title, String date, String start, String end) {
        return post(url).with(csrf()).with(user(me(who))).param("title", title).param("date", date)
                .param("startTime", start).param("endTime", end).param("location", "Library").param("description", "");
    }

    private static String base(Club c) { return "/dashboard/clubs/" + c.getClubId() + "/events"; }

    private static AccountPrincipal me(ClubLeader l) {
        return new AccountPrincipal(l.getLeaderId(), l.getEmail(), "Leader", "x", Role.LEADER, true);
    }

    private long denials() {
        return auditLogs.findAll().stream().filter(l -> l.getAction().equals(AuditLogger.OWNERSHIP_DENIED)).count();
    }

    private static ClubEvent event(Club club, String title, LocalDateTime start) {
        ClubEvent e = new ClubEvent();
        e.setClub(club);
        e.setTitle(title);
        e.setStartsAt(start);
        e.setEndsAt(start.plusHours(2));
        return e;
    }

    private static Club club(String name, Category category) {
        Club c = new Club();
        c.setClubName(name);
        c.setDescription("About " + name);
        c.setCategory(category);
        c.setContactEmail("contact@example.com");
        return c;
    }

    private static ClubLeader leader(String email, Club club) {
        ClubLeader l = new ClubLeader();
        l.setName("Leader " + email);
        l.setEmail(email);
        l.setPasswordHash("$2a$04$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234");
        l.setClub(club);
        return l;
    }
}
