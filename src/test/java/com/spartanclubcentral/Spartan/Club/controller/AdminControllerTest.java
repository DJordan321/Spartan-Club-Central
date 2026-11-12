package com.spartanclubcentral.Spartan.Club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The administrator's tools and who may use them (FR-11, FR-12, conflict C7, NFR-02). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private LeaderRepository leaders;
    @Autowired private RequestRepository requests;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordHasher hasher;
    @MockitoBean private JavaMailSender mailSender;

    private Club chess;
    private ClubLeader leader;
    private final AccountPrincipal admin = new AccountPrincipal(1L, "admin@example.com", "Administrator", "x", Role.ADMIN, true);

    @BeforeEach
    void setUp() {
        Category academic = categories.save(new Category("Academic"));
        chess = new Club();
        chess.setClubName("Chess Club");
        chess.setDescription("Games.");
        chess.setCategory(academic);
        chess.setContactEmail("chess@example.com");
        chess = clubs.save(chess);
        leader = new ClubLeader();
        leader.setName("Lee Leader");
        leader.setEmail("lee@example.com");
        leader.setPasswordHash(hasher.hash("leader-pass-1"));
        leader.setClub(chess);
        leader = leaders.save(leader);
        JoinRequest r = new JoinRequest();
        r.setClub(chess);
        r.setStudentName("Private Student");
        r.setStudentEmail("private.student@example.com");
        r.setMessage("A private message");
        requests.saveAndFlush(r);
    }

    @Test
    @DisplayName("Admin page lists clubs, leaders and the security log, but no student details (NFR-02)")
    void adminPage() throws Exception {
        mvc.perform(get("/admin").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Chess Club"), containsString("Lee Leader"),
                        containsString("lee@example.com"), containsString("Security log"),
                        not(containsString("private.student@example.com")), not(containsString("A private message")))));
    }

    @Test
    @DisplayName("Admin hides and shows any club (FR-12)")
    void hideAndShowClub() throws Exception {
        mvc.perform(post("/admin/clubs/" + chess.getClubId() + "/deactivate").with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin?clubHidden#clubs"));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/clubs/" + chess.getClubId() + "/activate").with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin?clubShown#clubs"));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(status().isOk());
        assertThat(auditLogs.findAll()).anyMatch(a -> a.getActorType().equals("ADMIN") && a.getAction().equals(AuditLogger.CLUB_DEACTIVATED));
    }

    @Test
    @DisplayName("Admin blocks a leader account (it can't log in), then unblocks it (conflict C7)")
    void blockAndUnblockLeader() throws Exception {
        mvc.perform(post("/admin/leaders/" + leader.getLeaderId() + "/disable").with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin?leaderDisabled#leaders"));
        mvc.perform(formLogin("/login").userParameter("email").user("lee@example.com").password("leader-pass-1"))
                .andExpect(unauthenticated());
        mvc.perform(post("/admin/leaders/" + leader.getLeaderId() + "/enable").with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin?leaderEnabled#leaders"));
        mvc.perform(formLogin("/login").userParameter("email").user("lee@example.com").password("leader-pass-1"))
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("Admin sends a reset link: the leader gets the email; the admin never sees the password (FR-11)")
    void sendResetLink() throws Exception {
        mvc.perform(post("/admin/leaders/" + leader.getLeaderId() + "/send-reset").with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin?resetSent#leaders"));
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertThat(auditLogs.findAll()).anyMatch(a -> a.getAction().equals(AuditLogger.RESET_LINK_SENT));
    }

    @Test
    @DisplayName("Only administrators: leaders get 403, logged-out visitors go to login, CSRF required")
    void onlyAdmins() throws Exception {
        AccountPrincipal asLeader = new AccountPrincipal(leader.getLeaderId(), "lee@example.com", "Lee", "x", Role.LEADER, true);
        mvc.perform(get("/admin").with(user(asLeader))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/clubs/" + chess.getClubId() + "/deactivate").with(csrf()).with(user(asLeader)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/leaders/" + leader.getLeaderId() + "/disable").with(csrf()))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/admin/clubs/" + chess.getClubId() + "/deactivate").with(user(admin)))
                .andExpect(status().isForbidden());
        assertThat(clubs.findById(chess.getClubId()).orElseThrow().isActive()).isTrue();
        assertThat(leaders.findById(leader.getLeaderId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Unknown club or leader id -> 404")
    void unknownIds() throws Exception {
        mvc.perform(post("/admin/clubs/999999/deactivate").with(csrf()).with(user(admin))).andExpect(status().isNotFound());
        mvc.perform(post("/admin/leaders/999999/send-reset").with(csrf()).with(user(admin))).andExpect(status().isNotFound());
    }
}
