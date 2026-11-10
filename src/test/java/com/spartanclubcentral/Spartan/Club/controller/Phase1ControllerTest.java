package com.spartanclubcentral.Spartan.Club.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Controller tests for the temporary Phase 1 checkpoint page (H2, no MySQL needed). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase1ControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClubRepository clubs;

    @Autowired
    private CategoryRepository categories;

    private Category academic;

    @BeforeEach
    void setUp() {
        academic = categories.save(new Category("Academic"));
        Category sports = categories.save(new Category("Sports"));
        clubs.save(club("Chess Club", "Weekly games.", academic));
        clubs.save(club("Engineering Builders Society", "We build robotics projects.", academic));
        clubs.save(club("Soccer Club", "Pickup games.", sports));
    }

    @Test
    void homeRedirectsToCheckpoint() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/phase-1"));
    }

    @Test
    void showsConnectedStatusAndAllClubs() throws Exception {
        mvc.perform(get("/phase-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Connected to")))
                .andExpect(content().string(containsString("Chess Club")))
                .andExpect(content().string(containsString("Soccer Club")));
    }

    @Test
    void searchFindsDescriptionMatch() throws Exception {
        mvc.perform(get("/phase-1").param("q", "robotics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Engineering Builders Society")))
                .andExpect(content().string(not(containsString("Soccer Club</th>"))));
    }

    @Test
    void keywordAndCategoryCombine() throws Exception {
        mvc.perform(get("/phase-1").param("q", "club").param("category", academic.getCategoryId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chess Club</th>")))
                .andExpect(content().string(not(containsString("Soccer Club</th>"))));
    }

    @Test
    void scriptInKeywordIsEscaped() throws Exception {
        mvc.perform(get("/phase-1").param("q", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("No clubs found")));
    }

    private static Club club(String name, String description, Category category) {
        Club c = new Club();
        c.setClubName(name);
        c.setDescription(description);
        c.setCategory(category);
        c.setContactEmail("contact@example.com");
        return c;
    }
}
