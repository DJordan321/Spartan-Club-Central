package com.spartanclubcentral.Spartan.Club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.model.RequestStatus;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full join request flow through Spring Security, the controller, services
 * and an H2 database. The mail server is replaced by a mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RequestControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private RequestRepository requests;
    @MockitoBean private JavaMailSender mailSender;

    private Club chess;
    private Club retro;

    @BeforeEach
    void setUp() {
        Category academic = categories.save(new Category("Academic"));
        chess = clubs.save(club("Chess Club", academic, true));
        retro = clubs.save(club("Retro Film Society", academic, false));
    }

    @Test
    @DisplayName("Profile page shows the join form with a CSRF token")
    void profileHasForm() throws Exception {
        mvc.perform(get("/clubs/" + chess.getClubId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("action=\"/clubs/" + chess.getClubId() + "/requests#join\""),
                        containsString("name=\"_csrf\""),
                        containsString("Send request"))));
    }

    @Test
    @DisplayName("TC-07/TC-09: valid request while logged out is saved, emailed, and confirmed")
    void validRequest() throws Exception {
        mvc.perform(submit(chess, "Jordan Lee", "jordan@example.com", "I'd love to join!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clubs/" + chess.getClubId() + "/requests/sent"));

        List<JoinRequest> saved = requests.findByClub_ClubIdOrderByDateSubmittedDesc(chess.getClubId());
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(RequestStatus.DELIVERED);
        assertThat(saved.get(0).getStudentEmail()).isEqualTo("jordan@example.com");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Confirmation page names the club")
    void confirmationPage() throws Exception {
        mvc.perform(get("/clubs/" + chess.getClubId() + "/requests/sent"))
                .andExpect(status().isOk())
                .andExpect(view().name("request-sent"))
                .andExpect(content().string(allOf(containsString("Your request was sent"), containsString("Chess Club"))));
    }

    @Test
    @DisplayName("TC-10: mail server down -> request still saved as FAILED, student still confirmed")
    void mailServerDown() throws Exception {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        mvc.perform(submit(chess, "Jordan Lee", "jordan@example.com", "Hello"))
                .andExpect(status().is3xxRedirection());

        assertThat(requests.findByClub_ClubIdOrderByDateSubmittedDesc(chess.getClubId()))
                .singleElement().extracting(JoinRequest::getStatus).isEqualTo(RequestStatus.FAILED);
    }

    @Test
    @DisplayName("TC-21: invalid input shows messages next to fields, keeps typed values, saves and sends nothing")
    void invalidRequest() throws Exception {
        mvc.perform(submit(chess, "Jordan Lee", "student@", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("club"))
                .andExpect(content().string(allOf(
                        containsString("Please fix the following"),
                        containsString("Enter a valid email address"),
                        containsString("Enter a message for the club."),
                        containsString("value=\"Jordan Lee\""),
                        containsString("aria-invalid=\"true\""))));

        assertThat(requests.count()).isZero();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Message over 1,000 characters is rejected")
    void tooLong() throws Exception {
        mvc.perform(submit(chess, "Jordan", "jordan@example.com", "m".repeat(1001)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1,000 characters or fewer")));
        assertThat(requests.count()).isZero();
    }

    @Test
    @DisplayName("SPR-05: script text is stored as plain text and shown escaped when re-displayed")
    void scriptIsPlainText() throws Exception {
        String script = "<script>alert(1)</script>";
        // Re-displayed after a validation error: must be escaped
        mvc.perform(submit(chess, "", "jordan@example.com", script))
                .andExpect(content().string(allOf(not(containsString(script)), containsString("&lt;script&gt;"))));
        // Valid submission: stored exactly as typed, as text
        mvc.perform(submit(chess, "Jordan", "jordan@example.com", script)).andExpect(status().is3xxRedirection());
        assertThat(requests.findAll()).singleElement().extracting(JoinRequest::getMessage).isEqualTo(script);
    }

    @Test
    @DisplayName("Form posted without the CSRF token is rejected")
    void missingCsrf() throws Exception {
        mvc.perform(post("/clubs/" + chess.getClubId() + "/requests")
                        .param("name", "Jordan").param("email", "jordan@example.com").param("message", "Hi"))
                .andExpect(status().isForbidden());
        assertThat(requests.count()).isZero();
    }

    @Test
    @DisplayName("Requests to inactive or unknown clubs get the 404 page and save nothing")
    void inactiveClub() throws Exception {
        mvc.perform(submit(retro, "Jordan", "jordan@example.com", "Hi"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
        mvc.perform(post("/clubs/999999/requests").with(csrf())
                        .param("name", "Jordan").param("email", "jordan@example.com").param("message", "Hi"))
                .andExpect(status().isNotFound());
        assertThat(requests.count()).isZero();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private static MockHttpServletRequestBuilder submit(Club club, String name, String email, String message) {
        return post("/clubs/" + club.getClubId() + "/requests").with(csrf())
                .param("name", name).param("email", email).param("message", message);
    }

    private static Club club(String name, Category category, boolean active) {
        Club c = new Club();
        c.setClubName(name);
        c.setDescription("About " + name);
        c.setCategory(category);
        c.setContactEmail("contact@example.com");
        c.setActive(active);
        return c;
    }
}
