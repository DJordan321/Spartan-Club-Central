package com.spartanclubcentral.Spartan.Club.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubEvent;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.EventRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.OfficerRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Renders the logged-in pages WITHOUT a test transaction, exactly like the
 * real app (open-in-view is off). This catches templates that read lazily
 * loaded data after the database session has closed, which the other tests,
 * running inside one transaction, can't see.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PageRenderWithoutTransactionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private LeaderRepository leaders;
    @Autowired private RequestRepository requests;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private OfficerRepository officers;
    @Autowired private EventRepository events;
    @Autowired private TransactionTemplate tx;

    private Club club;
    private ClubLeader leader;
    private Category category;

    @BeforeEach
    void setUp() {
        category = categories.save(new Category("RenderTest"));
        club = new Club();
        club.setClubName("Render Test Club");
        club.setDescription("Testing pages.");
        club.setCategory(category);
        club.setContactEmail("render@example.com");
        club = clubs.save(club);
        leader = new ClubLeader();
        leader.setName("Render Leader");
        leader.setEmail("render.leader@example.com");
        leader.setPasswordHash("$2a$04$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234");
        leader.setClub(club);
        leader = leaders.save(leader);
        JoinRequest r = new JoinRequest();
        r.setClub(club);
        r.setStudentName("Render Student");
        r.setStudentEmail("render.student@example.com");
        r.setMessage("Hello");
        requests.save(r);
        officers.save(new ClubOfficer(club, "Render Officer", "President", 1));
        ClubEvent e = new ClubEvent();
        e.setClub(club);
        e.setTitle("Render Event");
        e.setStartsAt(java.time.LocalDateTime.now().plusDays(2));
        events.save(e);
    }

    @AfterEach
    void cleanUp() {
        tx.executeWithoutResult(status -> {
            requests.deleteByClubId(club.getClubId());
            officers.deleteByClubId(club.getClubId());
            events.deleteByClubId(club.getClubId());
            leaders.deleteById(leader.getLeaderId());
            clubs.deleteById(club.getClubId());
            categories.deleteById(category.getCategoryId());
            auditLogs.deleteAll();
        });
    }

    @Test
    void publicClubPageShowsOfficers() throws Exception {
        mvc.perform(get("/clubs/" + club.getClubId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Render Officer"), containsString("Render Event"), containsString("</html>"))));
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Coming up this week"), containsString("Render Test Club"), containsString("</html>"))));
    }

    @Test
    void adminPageRendersCompletely() throws Exception {
        AccountPrincipal admin = new AccountPrincipal(1L, "admin@example.com", "Administrator", "x", Role.ADMIN, true);
        mvc.perform(get("/admin").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Render Leader"),
                        containsString("<td>Render Test Club</td>"), containsString("Security log"), containsString("</html>"))));
    }

    @Test
    void leaderPagesRenderCompletely() throws Exception {
        AccountPrincipal me = new AccountPrincipal(leader.getLeaderId(), leader.getEmail(), leader.getName(), "x", Role.LEADER, true);
        for (String url : new String[] {"/dashboard", "/dashboard/clubs/" + club.getClubId() + "/edit",
                "/dashboard/clubs/" + club.getClubId() + "/requests", "/dashboard/clubs/" + club.getClubId() + "/delete",
                "/dashboard/clubs/" + club.getClubId() + "/officers", "/dashboard/clubs/" + club.getClubId() + "/events",
                "/dashboard/clubs/" + club.getClubId() + "/events/new", "/dashboard/clubs/" + club.getClubId() + "/picture"}) {
            mvc.perform(get(url).with(user(me)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Render Test Club"), containsString("</html>"))));
        }
    }
}
