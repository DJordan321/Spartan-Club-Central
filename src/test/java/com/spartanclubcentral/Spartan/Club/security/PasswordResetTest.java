package com.spartanclubcentral.Spartan.Club.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.PasswordResetToken;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.PasswordResetTokenRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Forgot / reset password end to end, with the mail server mocked (FR-11, TC-15, TC-16). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetTest {

    private static final String EMAIL = "reset.me@example.com";
    private static final String OLD = "old-password-1";
    private static final String NEW = "brand-new-pass-2";

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaders;
    @Autowired private PasswordResetTokenRepository tokens;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordHasher hasher;
    @MockitoBean private JavaMailSender mailSender;

    private ClubLeader leader;

    @BeforeEach
    void setUp() {
        leader = new ClubLeader();
        leader.setName("Reset Me");
        leader.setEmail(EMAIL);
        leader.setPasswordHash(hasher.hash(OLD));
        leader = leaders.save(leader);
    }

    @Test
    @DisplayName("TC-15: forgot password -> emailed link -> new password works, old one doesn't")
    void fullResetFlow() throws Exception {
        String token = requestLinkAndCaptureToken(EMAIL);

        PasswordResetToken stored = tokens.findAll().get(0);
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(token);      // only the hash is stored
        assertThat(stored.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));

        mvc.perform(get("/reset-password").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Choose a new password"), containsString("name=\"_csrf\""))));
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("password", NEW).param("confirmPassword", NEW))
                .andExpect(redirectedUrl("/login?reset"));

        mvc.perform(formLogin("/login").userParameter("email").user(EMAIL).password(OLD)).andExpect(unauthenticated());
        mvc.perform(formLogin("/login").userParameter("email").user(EMAIL).password(NEW)).andExpect(authenticated());
        assertThat(auditLogs.findAll()).extracting(a -> a.getAction())
                .contains(AuditLogger.PASSWORD_RESET_REQUESTED, AuditLogger.PASSWORD_RESET_COMPLETED);
        mvc.perform(get("/login").param("reset", "")).andExpect(content().string(containsString("Your password was changed.")));
    }

    @Test
    @DisplayName("TC-16: a link works only once")
    void linkWorksOnce() throws Exception {
        String token = requestLinkAndCaptureToken(EMAIL);
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                .param("password", NEW).param("confirmPassword", NEW)).andExpect(redirectedUrl("/login?reset"));
        mvc.perform(get("/reset-password").param("token", token))
                .andExpect(content().string(containsString("This reset link has expired or was already used.")));
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("password", "third-password-3").param("confirmPassword", "third-password-3"))
                .andExpect(content().string(containsString("expired or was already used")));
        assertThat(hasher.matches(NEW, leaders.findById(leader.getLeaderId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("TC-16: an expired link is refused")
    void expiredLink() throws Exception {
        String token = requestLinkAndCaptureToken(EMAIL);
        PasswordResetToken stored = tokens.findAll().get(0);
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokens.saveAndFlush(stored);
        mvc.perform(get("/reset-password").param("token", token))
                .andExpect(content().string(containsString("This link doesn")));
    }

    @Test
    @DisplayName("Asking again cancels the older link")
    void newLinkCancelsOld() throws Exception {
        String first = requestLinkAndCaptureToken(EMAIL);
        clearInvocations(mailSender);
        String second = requestLinkAndCaptureToken(EMAIL);
        mvc.perform(get("/reset-password").param("token", first)).andExpect(content().string(containsString("This link doesn")));
        mvc.perform(get("/reset-password").param("token", second)).andExpect(content().string(containsString("Choose a new password")));
    }

    @Test
    @DisplayName("Unknown email gets the same answer and no email is sent (no account discovery)")
    void unknownEmail() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "nobody@example.com"))
                .andExpect(redirectedUrl("/forgot-password?sent"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(tokens.count()).isZero();
        mvc.perform(get("/forgot-password").param("sent", ""))
                .andExpect(content().string(containsString("If that email belongs to a club leader account")));
    }

    @Test
    @DisplayName("Password rules apply; passwords are never sent back to the page")
    void passwordRules() throws Exception {
        String token = requestLinkAndCaptureToken(EMAIL);
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("password", "short").param("confirmPassword", "short"))
                .andExpect(content().string(allOf(containsString("Password must be at least 8 characters."),
                        not(containsString("value=\"short\"")))));
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("password", NEW).param("confirmPassword", "different-pass-9"))
                .andExpect(content().string(containsString("The two passwords don&#39;t match.")));
        assertThat(hasher.matches(OLD, leaders.findById(leader.getLeaderId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("A blocked account gets no reset email and its old links stop working")
    void blockedAccount() throws Exception {
        String token = requestLinkAndCaptureToken(EMAIL);
        leader.setEnabled(false);
        leaders.saveAndFlush(leader);
        mvc.perform(get("/reset-password").param("token", token)).andExpect(content().string(containsString("This link doesn")));
        clearInvocations(mailSender);
        mvc.perform(post("/forgot-password").with(csrf()).param("email", EMAIL)).andExpect(redirectedUrl("/forgot-password?sent"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Forgot/reset forms need the CSRF token; a made-up token shows the 'doesn't work' page")
    void guards() throws Exception {
        mvc.perform(post("/forgot-password").param("email", EMAIL)).andExpect(status().isForbidden());
        mvc.perform(get("/reset-password").param("token", "made-up-token")).andExpect(content().string(containsString("This link doesn")));
        mvc.perform(get("/reset-password")).andExpect(content().string(containsString("This link doesn")));
    }

    /** Submits "forgot password" and pulls the token out of the (mocked) email. */
    private String requestLinkAndCaptureToken(String email) throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/forgot-password?sent"));
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        SimpleMailMessage mail = sent.getValue();
        assertThat(mail.getTo()).containsExactly(email);
        assertThat(mail.getSubject()).isEqualTo("Reset your Spartan Club Central password");
        Matcher m = Pattern.compile("http://localhost:8080/reset-password\\?token=([A-Za-z0-9_-]+)").matcher(mail.getText());
        assertThat(m.find()).as("email contains the reset link").isTrue();
        return m.group(1);
    }
}
