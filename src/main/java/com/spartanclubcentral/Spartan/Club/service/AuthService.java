package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.RegistrationForm;
import com.spartanclubcentral.Spartan.Club.model.Administrator;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * Phase 6 adds password reset here.
 */
@Service
public class AuthService implements UserDetailsService {

    private final LeaderRepository leaders;
    private final AdminRepository admins;
    private final PasswordHasher passwordHasher;
    private final RegistrationValidator validator;
    private final AuditLogger audit;

    public AuthService(LeaderRepository leaders, AdminRepository admins, PasswordHasher passwordHasher,
                       RegistrationValidator validator, AuditLogger audit) {
        this.leaders = leaders;
        this.admins = admins;
        this.passwordHasher = passwordHasher;
        this.validator = validator;
        this.audit = audit;
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

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
