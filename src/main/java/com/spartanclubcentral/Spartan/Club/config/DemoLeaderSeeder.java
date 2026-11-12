package com.spartanclubcentral.Spartan.Club.config;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DEMO ONLY: creates a ready-made club leader account for class demonstrations.
 *
 * Active only with the "demo" Spring profile
 * (SPRING_PROFILES_ACTIVE=demo). Without it this class does nothing,
 * so these public, well-known credentials never work on a real deployment.
 *
 *   Email:    demo.leader@clubcentral.local
 *   Password: spartan-demo-2026
 *
 * The password is still stored only as a BCrypt hash (NFR-01). The account
 * is linked to Chess Club (if no other leader has it) so the Phase 5
 * dashboard has a club to edit and requests to show.
 */
@Component
@Profile("demo")
public class DemoLeaderSeeder implements ApplicationRunner {

    public static final String EMAIL = "demo.leader@clubcentral.local";
    public static final String PASSWORD = "spartan-demo-2026";
    public static final String NAME = "Demo Leader";
    public static final String CLUB_NAME = "Chess Club";

    private static final Logger log = LoggerFactory.getLogger(DemoLeaderSeeder.class);

    private final LeaderRepository leaders;
    private final ClubRepository clubs;
    private final AuthService authService;
    private final PasswordHasher passwordHasher;
    private final AuditLogger audit;

    public DemoLeaderSeeder(LeaderRepository leaders, ClubRepository clubs, AuthService authService,
                            PasswordHasher passwordHasher, AuditLogger audit) {
        this.leaders = leaders;
        this.clubs = clubs;
        this.authService = authService;
        this.passwordHasher = passwordHasher;
        this.audit = audit;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /** @return true if the demo leader was created */
    @Transactional
    boolean seed() {
        if (authService.isEmailTaken(EMAIL)) {
            log.info("Demo mode: demo leader already exists ({}).", EMAIL);
            return false;
        }
        ClubLeader leader = new ClubLeader();
        leader.setName(NAME);
        leader.setEmail(EMAIL);
        leader.setPosition("President");
        leader.setPasswordHash(passwordHasher.hash(PASSWORD));

        Optional<Club> club = clubs.findByClubNameIgnoreCase(CLUB_NAME)
                .filter(c -> leaders.findByClub_ClubId(c.getClubId()).isEmpty());
        club.ifPresent(leader::setClub);

        leader = leaders.save(leader);
        audit.log("LEADER", leader.getLeaderId(), AuditLogger.DEMO_SEEDED, "LEADER", leader.getLeaderId(), true);
        log.warn("Demo mode: created demo leader {}{}. Never use the demo profile outside local demos.",
                EMAIL, club.isPresent() ? " (linked to " + CLUB_NAME + ")" : "");
        return true;
    }
}
