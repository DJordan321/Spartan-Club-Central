package com.spartanclubcentral.Spartan.Club.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for the public student pages (H2, no MySQL needed).
 * Each test runs in a transaction that is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClubControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private RequestRepository requests;

    private Category academic;
    private Category sports;
    private Club chess;
    private Club builders;
    private Club retro;

    @BeforeEach
    void setUp() {
        academic = categories.save(new Category("Academic"));
        sports = categories.save(new Category("Sports"));
        chess = clubs.save(club("Chess Club", "Weekly games and tournaments.", academic, true));
        chess.setMeetingTime("Tuesdays 6:00 PM, Library 210");
        chess.setContactInfo("Boards provided.");
        chess.setInstagramUrl("https://example.com/instagram/chess");
        chess.setWebsiteUrl("https://example.com/chess");
        builders = clubs.save(club("Engineering Builders Society", "We build robots for robotics contests.", academic, true));
        clubs.save(club("Soccer Club", "Pickup games.", sports, true));
        retro = clubs.save(club("Retro Film Society", "Classic films.", sports, false));
        clubs.flush();
    }

    // ---------- Homepage (NFR-03) ----------

    @Test
    @DisplayName("Homepage shows the search box and one link per category with counts")
    void homepage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(allOf(
                        containsString("Find your club at NSU"),
                        containsString("action=\"/clubs\""),
                        containsString("href=\"/clubs?category=" + academic.getCategoryId() + "\""),
                        containsString("2 clubs"),      // Academic: Chess + Builders
                        containsString("1 club<"))));   // Sports: Soccer (inactive Retro not counted)
    }

    // ---------- Directory (FR-01, FR-02) ----------

    @Test
    @DisplayName("Directory with no filters lists every active club, never inactive ones")
    void directoryListsActiveClubs() throws Exception {
        mvc.perform(get("/clubs"))
                .andExpect(status().isOk())
                .andExpect(view().name("directory"))
                .andExpect(content().string(allOf(
                        containsString("Chess Club"), containsString("Soccer Club"),
                        containsString("3 clubs"), not(containsString("Retro Film Society")))));
    }

    @Test
    @DisplayName("TC-02: keyword found in the description only")
    void keywordInDescription() throws Exception {
        mvc.perform(get("/clubs").param("q", "robotics"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Engineering Builders Society"),
                        containsString("1 club<"),
                        not(containsString("Chess Club")))));
    }

    @Test
    @DisplayName("TC-05: keyword and category combine, and both stay selected")
    void keywordAndCategory() throws Exception {
        mvc.perform(get("/clubs").param("q", "club").param("category", academic.getCategoryId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString(">Chess Club<"),
                        not(containsString(">Soccer Club<")),
                        containsString("value=\"club\""),
                        containsString("selected=\"selected\""),
                        containsString(" in <span>Academic</span>"))));
    }

    @Test
    @DisplayName("TC-03: no match shows a friendly empty state")
    void noMatch() throws Exception {
        mvc.perform(get("/clubs").param("q", "zzzzqq"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("No clubs found"), containsString("0 clubs"))));
    }

    @Test
    @DisplayName("SPR-05: script text in the search box is shown as text, never run")
    void scriptIsEscaped() throws Exception {
        mvc.perform(get("/clubs").param("q", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("<script>alert(1)</script>")),
                        containsString("&lt;script&gt;"))));
    }

    @Test
    @DisplayName("Malformed category value shows the 404 page, not an error dump")
    void badCategoryValue() throws Exception {
        mvc.perform(get("/clubs").param("category", "abc"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    // ---------- Profile (FR-05, FR-06, TC-06) ----------

    @Test
    @DisplayName("TC-06: profile shows description, meeting time, contact info and social links")
    void profileShowsEverything() throws Exception {
        mvc.perform(get("/clubs/" + chess.getClubId()))
                .andExpect(status().isOk())
                .andExpect(view().name("club"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Chess Club</h1>"),
                        containsString("Weekly games and tournaments."),
                        containsString("Tuesdays 6:00 PM, Library 210"),
                        containsString("contact@example.com"),
                        containsString("Boards provided."),
                        containsString("href=\"https://example.com/instagram/chess\""),
                        containsString("href=\"https://example.com/chess\""),
                        containsString("rel=\"noopener noreferrer\""))));
    }

    @Test
    @DisplayName("NFR-02: a club's public page never shows students' request details")
    void profileHidesStudentData() throws Exception {
        JoinRequest r = new JoinRequest();
        r.setClub(builders);
        r.setStudentName("Private Student");
        r.setStudentEmail("private.student@example.com");
        r.setMessage("Secret message");
        requests.saveAndFlush(r);

        mvc.perform(get("/clubs/" + builders.getClubId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("Private Student")),
                        not(containsString("private.student@example.com")),
                        not(containsString("Secret message")))));
    }

    @Test
    @DisplayName("FR-12: an inactive club's profile is 'not found'")
    void inactiveClubIsNotFound() throws Exception {
        mvc.perform(get("/clubs/" + retro.getClubId()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(allOf(
                        containsString("Page not found"), not(containsString("Retro Film Society")))));
    }

    @Test
    @DisplayName("SPR-06: unknown or malformed club ids show the friendly 404 with no technical details")
    void unknownClub() throws Exception {
        for (String path : new String[] {"/clubs/999999", "/clubs/abc"}) {
            mvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(allOf(
                            containsString("Page not found"),
                            not(containsString("Exception")),
                            not(containsString("at com.")))));
        }
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
}
