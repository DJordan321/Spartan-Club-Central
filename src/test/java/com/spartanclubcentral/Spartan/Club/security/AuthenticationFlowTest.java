package com.spartanclubcentral.Spartan.Club.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spartanclubcentral.Spartan.Club.model.Administrator;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login, logout and "who may open what" through the real Spring Security
 * setup (FR-04, SPR-01, SPR-06, SPR-07, SPR-08). H2 database, no MySQL needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationFlowTest {

    private static final String LEADER_EMAIL = "leader@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String PASSWORD = "correct-horse-1";

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaders;
    @Autowired private AdminRepository admins;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordHasher hasher;

    private ClubLeader leader;
    private Administrator admin;

    @BeforeEach
    void setUp() {
        leader = new ClubLeader();
        leader.setName("Jordan Lee");
        leader.setEmail(LEADER_EMAIL);
        leader.setPosition("President");
        leader.setPasswordHash(hasher.hash(PASSWORD));
        leader = leaders.save(leader);

        admin = new Administrator();
        admin.setName("Administrator");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(hasher.hash(PASSWORD));
        admin = admins.save(admin);
    }

    // ---------- SPR-01 / TC-17: logged-out visitors can't reach protected pages ----------

    @Test
    @DisplayName("TC-17: logged out, /dashboard and /admin send you to the login page")
    void protectedPagesRedirectToLogin() throws Exception {
        for (String url : new String[] {"/dashboard", "/dashboard/anything", "/admin", "/admin/anything"}) {
            mvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    @DisplayName("Public pages stay public; unknown pages show 404; other methods are blocked")
    void publicPagesAndDefaults() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/clubs")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/css/main.css")).andExpect(status().isOk());
        mvc.perform(get("/nope")).andExpect(status().isNotFound());
        mvc.perform(delete("/clubs/1").with(csrf())).andExpect(status().is3xxRedirection());
    }

    // ---------- FR-04: logging in ----------

    @Test
    @DisplayName("Leader logs in -> /dashboard with ROLE_LEADER; audit LOGIN_SUCCESS")
    void leaderLogin() throws Exception {
        long before = auditLogs.count();
        mvc.perform(formLogin("/login").userParameter("email").user("  LEADER@example.com ").password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withRoles("LEADER"));
        assertThat(auditLogs.count()).isEqualTo(before + 1);
        assertThat(lastAction()).isEqualTo(AuditLogger.LOGIN_SUCCESS);
    }

    @Test
    @DisplayName("Administrator logs in -> /admin with ROLE_ADMIN")
    void adminLogin() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(ADMIN_EMAIL).password(PASSWORD))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    @DisplayName("SPR-06: wrong password and unknown email get the same generic message; audit LOGIN_FAILED")
    void failedLogin() throws Exception {
        long before = auditLogs.count();
        mvc.perform(formLogin("/login").userParameter("email").user(LEADER_EMAIL).password("wrong-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin("/login").userParameter("email").user("nobody@example.com").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        assertThat(auditLogs.count()).isEqualTo(before + 2);
        assertThat(lastAction()).isEqualTo(AuditLogger.LOGIN_FAILED);

        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Invalid email or password.")));
    }

    @Test
    @DisplayName("A disabled leader account can't log in")
    void disabledLeader() throws Exception {
        leader.setEnabled(false);
        leaders.saveAndFlush(leader);
        mvc.perform(formLogin("/login").userParameter("email").user(LEADER_EMAIL).password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("After login you return to the protected page you asked for, but only in your own area")
    void returnsToSavedPageOnlyInOwnArea() throws Exception {
        // Leader asked for /dashboard first: goes back there.
        MockHttpSession leaderSession = new MockHttpSession();
        mvc.perform(get("/dashboard").session(leaderSession)).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/login").with(csrf()).session(leaderSession)
                        .param("email", LEADER_EMAIL).param("password", PASSWORD))
                .andExpect(redirectedUrl("http://localhost/dashboard?continue"));

        // Someone asked for /dashboard, then the ADMIN logs in on that browser: admin goes to /admin, not a 403.
        MockHttpSession adminSession = new MockHttpSession();
        mvc.perform(get("/dashboard").session(adminSession)).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/login").with(csrf()).session(adminSession)
                        .param("email", ADMIN_EMAIL).param("password", PASSWORD))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    @DisplayName("Login form without the CSRF token is rejected")
    void loginWithoutCsrf() throws Exception {
        mvc.perform(post("/login").param("email", LEADER_EMAIL).param("password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    // ---------- Roles ----------

    @Test
    @DisplayName("Leader sees their dashboard; leader can't open /admin; admin can't open /dashboard")
    void roleChecks() throws Exception {
        mvc.perform(get("/dashboard").with(user(leaderPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Welcome, <span>Jordan Lee</span>"),
                        containsString("President"), containsString("Log out"), not(containsString("Leader login")))));
        mvc.perform(get("/admin").with(user(leaderPrincipal()))).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard").with(user(adminPrincipal()))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administrator")));
    }

    @Test
    @DisplayName("Logged-in users visiting /login or /register go to their home page")
    void loggedInSkipsLogin() throws Exception {
        mvc.perform(get("/login").with(user(leaderPrincipal()))).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(get("/register").with(user(adminPrincipal()))).andExpect(redirectedUrl("/admin"));
    }

    @Test
    @DisplayName("Header shows 'Leader login' when logged out")
    void headerLoggedOut() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(allOf(
                containsString("Leader login"), not(containsString("Log out")))));
    }

    // ---------- Logout ----------

    @Test
    @DisplayName("Log out (POST with CSRF) ends the session; audit LOGOUT")
    void logoutFlow() throws Exception {
        MvcResult login = mvc.perform(formLogin("/login").userParameter("email").user(LEADER_EMAIL).password(PASSWORD))
                .andExpect(authenticated()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        mvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
        assertThat(lastAction()).isEqualTo(AuditLogger.LOGOUT);
        mvc.perform(get("/dashboard").session(session)).andExpect(redirectedUrl("/login"));
    }

    private AccountPrincipal leaderPrincipal() {
        return new AccountPrincipal(leader.getLeaderId(), LEADER_EMAIL, "Jordan Lee", "x", Role.LEADER, true);
    }

    private AccountPrincipal adminPrincipal() {
        return new AccountPrincipal(admin.getAdminId(), ADMIN_EMAIL, "Administrator", "x", Role.ADMIN, true);
    }

    private String lastAction() {
        return auditLogs.findTop50ByOrderByEventTimeDesc().stream()
                .max((a, b) -> Long.compare(a.getLogId(), b.getLogId()))
                .orElseThrow().getAction();
    }
}
