package com.spartanclubcentral.Spartan.Club.config;

import com.spartanclubcentral.Spartan.Club.model.Administrator;
import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.EmailRules;
import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the one administrator account at startup from the ADMIN_EMAIL and
 * ADMIN_PASSWORD environment variables.
 *
 * Why here and not in a SQL file: the password must be BCrypt-hashed by the
 * app, and the repository is public, so neither the password nor its hash
 * should ever be committed. Runs only when no administrator exists yet.
 * Problems are logged and skipped so the site still starts.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminRepository admins;
    private final AuthService authService;
    private final PasswordHasher passwordHasher;
    private final AuditLogger audit;
    private final String email;
    private final String password;

    public AdminSeeder(AdminRepository admins, AuthService authService, PasswordHasher passwordHasher,
                       AuditLogger audit,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.password:}") String password) {
        this.admins = admins;
        this.authService = authService;
        this.passwordHasher = passwordHasher;
        this.audit = audit;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /** @return true if an administrator account was created */
    boolean seed() {
        if (admins.count() > 0) {
            return false;
        }
        if (email.isEmpty() || password.isEmpty()) {
            log.info("No administrator yet. Set ADMIN_EMAIL and ADMIN_PASSWORD to create one at startup.");
            return false;
        }
        if (!EmailRules.isValid(email)) {
            log.warn("ADMIN_EMAIL is not a valid email address; administrator not created.");
            return false;
        }
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < FieldLimits.PASSWORD_MIN || bytes > FieldLimits.PASSWORD_MAX) {
            log.warn("ADMIN_PASSWORD must be 8 to 72 characters; administrator not created.");
            return false;
        }
        if (authService.isEmailTaken(email)) {
            log.warn("ADMIN_EMAIL already belongs to a leader account; administrator not created.");
            return false;
        }
        Administrator admin = new Administrator();
        admin.setName("Administrator");
        admin.setEmail(email);
        admin.setPasswordHash(passwordHasher.hash(password));
        admin = admins.save(admin);
        audit.log("ADMIN", admin.getAdminId(), AuditLogger.ADMIN_SEEDED, null, null, true);
        log.info("Administrator account created from ADMIN_EMAIL.");
        return true;
    }
}
