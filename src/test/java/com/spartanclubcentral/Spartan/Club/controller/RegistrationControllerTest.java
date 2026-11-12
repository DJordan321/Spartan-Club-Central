package com.spartanclubcentral.Spartan.Club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.spartanclubcentral.Spartan.Club.model.Administrator;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Leader registration through the real controller, services and H2 (FR-04, NFR-01, TC-08). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaders;
    @Autowired private AdminRepository admins;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordHasher hasher;

    @Test
    @DisplayName("Registration page shows the form with a CSRF token")
    void form() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Create a leader account"),
                        containsString("name=\"_csrf\""), containsString("autocomplete=\"new-password\""))));
    }

    @Test
    @DisplayName("TC-08 / TC-18: valid registration stores only a BCrypt hash, then goes to login")
    void validRegistration() throws Exception {
        long auditBefore = auditLogs.count();
        mvc.perform(register("Jordan Lee", "  Jordan.Lee@Example.com ", "President", "correct-horse-1", "correct-horse-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        ClubLeader saved = leaders.findByEmailIgnoreCase("jordan.lee@example.com").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("jordan.lee@example.com");   // trimmed + lowercase
        assertThat(saved.getName()).isEqualTo("Jordan Lee");
        assertThat(saved.getPosition()).isEqualTo("President");
        assertThat(saved.getClubId()).isNull();                            // creates the club in Phase 5
        assertThat(saved.getPasswordHash()).startsWith("$2a$12$").hasSize(60).doesNotContain("correct-horse-1");
        assertThat(hasher.matches("correct-horse-1", saved.getPasswordHash())).isTrue();
        assertThat(auditLogs.count()).isEqualTo(auditBefore + 1);
        assertThat(auditLogs.findTop50ByOrderByEventTimeDesc()).anyMatch(a -> a.getAction().equals(AuditLogger.REGISTERED));
    }

    @Test
    @DisplayName("Mistakes show messages, keep name/email, and never send passwords back")
    void invalidRegistration() throws Exception {
        mvc.perform(register("Jordan Lee", "jordan@", "", "short", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(allOf(
                        containsString("Please fix the following"),
                        containsString("Enter a valid email address"),
                        containsString("Password must be at least 8 characters."),
                        containsString("value=\"Jordan Lee\""),
                        containsString("value=\"jordan@\""),
                        not(containsString("value=\"short\"")))));
        assertThat(leaders.count()).isZero();
    }

    @Test
    @DisplayName("Passwords that don't match are rejected")
    void mismatch() throws Exception {
        mvc.perform(register("Jordan", "jordan@example.com", "", "correct-horse-1", "correct-horse-2"))
                .andExpect(content().string(allOf(containsString("The two passwords don&#39;t match."),
                        not(containsString("correct-horse")))));
        assertThat(leaders.count()).isZero();
    }

    @Test
    @DisplayName("An email can only have one account (leader or admin, any capitals)")
    void duplicateEmail() throws Exception {
        mvc.perform(register("Jordan", "jordan@example.com", "", "correct-horse-1", "correct-horse-1"))
                .andExpect(redirectedUrl("/login?registered"));
        mvc.perform(register("Someone Else", "JORDAN@example.com", "", "another-pass-1", "another-pass-1"))
                .andExpect(content().string(containsString("That email already has an account. Log in instead.")));

        Administrator admin = new Administrator();
        admin.setName("Administrator");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(hasher.hash("admin-pass-1"));
        admins.save(admin);
        mvc.perform(register("Sneaky", "admin@example.com", "", "another-pass-1", "another-pass-1"))
                .andExpect(content().string(containsString("That email already has an account.")));
        assertThat(leaders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Registration without the CSRF token is rejected")
    void noCsrf() throws Exception {
        mvc.perform(post("/register").param("name", "Jordan").param("email", "jordan@example.com")
                        .param("password", "correct-horse-1").param("confirmPassword", "correct-horse-1"))
                .andExpect(status().isForbidden());
        assertThat(leaders.count()).isZero();
    }

    @Test
    @DisplayName("SPR-05: script text in the name is shown as text, never run")
    void scriptEscaped() throws Exception {
        mvc.perform(register("<script>alert(1)</script>", "bad", "", "", ""))
                .andExpect(content().string(allOf(not(containsString("<script>alert(1)</script>")),
                        containsString("&lt;script&gt;"))));
    }

    private static MockHttpServletRequestBuilder register(String name, String email, String position,
                                                          String password, String confirm) {
        return post("/register").with(csrf())
                .param("name", name).param("email", email).param("position", position)
                .param("password", password).param("confirmPassword", confirm);
    }
}
