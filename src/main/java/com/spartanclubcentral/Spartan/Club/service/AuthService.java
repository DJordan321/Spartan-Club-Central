package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.RegistrationForm;
import com.spartanclubcentral.Spartan.Club.dto.ResetPasswordForm;
import com.spartanclubcentral.Spartan.Club.model.Administrator;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.PasswordResetToken;
import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.PasswordResetTokenRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: club leader accounts and login lookup (FR-04, NFR-01).
 *
 * Login itself (checking the password, creating the session) is done by
 * Spring Security, which asks this class to find the account by email.
 * Registration is done here: validate, make sure the email is free, then
 * store ONLY the BCrypt hash of the password (SPR-02).
 *
 * Password reset (FR-11): a random one-time token is emailed as a link; only
 * its SHA-256 hash is stored, it expires after 30 minutes, works once, and a
 * new request cancels older links.
 */
@Service
public class AuthService implements UserDetailsService {

    private final LeaderRepository leaders;
    private final AdminRepository admins;
    private final PasswordHasher passwordHasher;
    private final RegistrationValidator validator;
    private final AuditLogger audit;
    private final PasswordResetTokenRepository resetTokens;
    private final NotificationService notifications;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    /** How long a reset link works. */
    public static final int RESET_MINUTES = 30;

    public AuthService(LeaderRepository leaders, AdminRepository admins, PasswordHasher passwordHasher,
                       RegistrationValidator validator, AuditLogger audit,
                       PasswordResetTokenRepository resetTokens, NotificationService notifications,
                       @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.leaders = leaders;
        this.admins = admins;
        this.passwordHasher = passwordHasher;
        this.validator = validator;
        this.audit = audit;
        this.resetTokens = resetTokens;
        this.notifications = notifications;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Called by Spring Security at login. Looks in club_leader first, then
     * administrator. The same "not found" result for every miss means the
     * login page can't be used to discover which emails have accounts.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = normalizeEmail(email);
        Optional<ClubLeader> leader = leaders.findByEmailIgnoreCase(normalized);
        if (leader.isPresent()) {
            ClubLeader l = leader.get();
            return new AccountPrincipal(l.getLeaderId(), l.getEmail(), l.getName(), l.getPasswordHash(),
                    Role.LEADER, l.isEnabled());
        }
        Optional<Administrator> admin = admins.findByEmailIgnoreCase(normalized);
        if (admin.isPresent()) {
            Administrator a = admin.get();
            return new AccountPrincipal(a.getAdminId(), a.getEmail(), a.getName(), a.getPasswordHash(),
                    Role.ADMIN, true);
        }
        throw new UsernameNotFoundException("No account");
    }

    /**
     * Creates a club leader account. The leader has no club yet; they create
     * it from the dashboard in Phase 5 (FR-08).
     *
     * @throws ValidationException with one message per problem field
     */
    @Transactional
    public ClubLeader register(RegistrationForm form) {
        Map<String, String> errors = validator.validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String email = normalizeEmail(form.getEmail());
        if (isEmailTaken(email)) {
            // Telling the user is friendlier than a silent failure; for a club
            // directory, revealing that an email has an account is acceptable.
            throw new ValidationException(Map.of("email",
                    "That email already has an account. Log in instead."));
        }

        ClubLeader leader = new ClubLeader();
        leader.setName(RequestValidator.trim(form.getName()));
        leader.setEmail(email);
        String position = RequestValidator.trim(form.getPosition());
        leader.setPosition(position.isEmpty() ? null : position);
        leader.setPasswordHash(passwordHasher.hash(form.getPassword()));
        leader = leaders.save(leader);

        audit.log("LEADER", leader.getLeaderId(), AuditLogger.REGISTERED, "LEADER", leader.getLeaderId(), true);
        return leader;
    }

    /** The leader behind a logged-in session. */
    @Transactional(readOnly = true)
    public ClubLeader getLeader(Long leaderId) {
        return leaders.findById(leaderId)
                .orElseThrow(() -> new NotFoundException("Leader not found"));
    }

    /** An email may belong to only one account, leader or administrator. */
    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        String normalized = normalizeEmail(email);
        return leaders.existsByEmailIgnoreCase(normalized) || admins.findByEmailIgnoreCase(normalized).isPresent();
    }

    // ===================== Password reset (FR-11) =====================

    /**
     * "Forgot password": emails a reset link if this email belongs to an
     * enabled leader account. The caller always shows the SAME message, so
     * the page can't be used to find out which emails have accounts.
     * Administrator accounts are recovered with ADMIN_PASSWORD instead.
     */
    @Transactional
    public void requestReset(String email) {
        leaders.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(ClubLeader::isEnabled)
                .ifPresent(leader -> {
                    sendResetLink(leader);
                    audit.log("LEADER", leader.getLeaderId(), AuditLogger.PASSWORD_RESET_REQUESTED,
                            "LEADER", leader.getLeaderId(), true);
                });
    }

    /**
     * Creates a new one-time link for this leader, cancels older unused
     * links, and emails it. Used by "forgot password" and by the
     * administrator's "send reset link" button.
     *
     * @return true if the email was accepted by the mail server
     */
    @Transactional
    public boolean sendResetLink(ClubLeader leader) {
        LocalDateTime now = LocalDateTime.now();
        for (PasswordResetToken old : resetTokens.findByLeader_LeaderIdAndUsedAtIsNull(leader.getLeaderId())) {
            old.setUsedAt(now);              // an older link stops working
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setLeader(leader);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(RESET_MINUTES));
        resetTokens.save(token);

        String link = baseUrl + "/reset-password?token=" + rawToken;
        return notifications.sendResetLink(leader.getEmail(), leader.getName(), link);
    }

    /** True if the link is real, unused, not expired, and its account is still enabled. */
    @Transactional(readOnly = true)
    public boolean isResetTokenValid(String rawToken) {
        return findUsableToken(rawToken).isPresent();
    }

    /**
     * Sets a new password from a reset link. The link is used up, so it can't
     * be used twice.
     *
     * @throws ValidationException "token" if the link is invalid/expired/used,
     *         or password/confirmPassword messages
     */
    @Transactional
    public void resetPassword(ResetPasswordForm form) {
        PasswordResetToken token = findUsableToken(form.getToken()).orElseThrow(() ->
                new ValidationException(Map.of("token", "This reset link has expired or was already used.")));
        Map<String, String> errors = validator.validatePassword(form.getPassword(), form.getConfirmPassword());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        ClubLeader leader = token.getLeader();
        leader.setPasswordHash(passwordHasher.hash(form.getPassword()));
        leaders.save(leader);
        token.setUsedAt(LocalDateTime.now());
        resetTokens.save(token);
        audit.log("LEADER", leader.getLeaderId(), AuditLogger.PASSWORD_RESET_COMPLETED,
                "LEADER", leader.getLeaderId(), true);
    }

    private Optional<PasswordResetToken> findUsableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 100) {
            return Optional.empty();
        }
        return resetTokens.findByTokenHash(sha256(rawToken.trim()))
                .filter(t -> t.isUsable(LocalDateTime.now()))
                .filter(t -> t.getLeader().isEnabled());
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
