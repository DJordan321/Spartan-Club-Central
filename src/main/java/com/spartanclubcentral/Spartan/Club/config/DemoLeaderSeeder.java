package com.spartanclubcentral.Spartan.Club.config;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubEvent;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.model.RequestStatus;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.EventRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.OfficerRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import java.time.LocalDateTime;
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
 * dashboard has a club to edit, plus three made-up join requests so the
 * request inbox isn't empty during a demo, and three officers (FR-13).
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
    private final RequestRepository requests;
    private final OfficerRepository officers;
    private final EventRepository events;
    private final AuthService authService;
    private final PasswordHasher passwordHasher;
    private final AuditLogger audit;

    public DemoLeaderSeeder(LeaderRepository leaders, ClubRepository clubs, RequestRepository requests,
                            OfficerRepository officers, EventRepository events,
                            AuthService authService,
                            PasswordHasher passwordHasher, AuditLogger audit) {
        this.leaders = leaders;
        this.clubs = clubs;
        this.requests = requests;
        this.officers = officers;
        this.events = events;
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
            log.info("Demo mode: the demo leader account already exists.");
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
        club.filter(c -> requests.countByClub_ClubId(c.getClubId()) == 0).ifPresent(this::addSampleRequests);
        club.filter(c -> officers.countByClub_ClubId(c.getClubId()) == 0).ifPresent(c -> {
            officers.save(new ClubOfficer(c, NAME, "President", 1));
            officers.save(new ClubOfficer(c, "Priya Patel", "Treasurer", 2));
            officers.save(new ClubOfficer(c, "Marcus Green", "Tournament Director", 3));
        });
        club.filter(c -> events.countByClub_ClubId(c.getClubId()) == 0).ifPresent(this::addSampleEvents);

        leader = leaders.save(leader);
        audit.log("LEADER", leader.getLeaderId(), AuditLogger.DEMO_SEEDED, "LEADER", leader.getLeaderId(), true);
        // No email in the log (SPR-08); the credentials are in docs/run-locally.md.
        log.warn("Demo mode: created the demo leader account{}. Never use the demo profile outside local demos.",
                club.isPresent() ? " (linked to " + CLUB_NAME + ")" : "");
        return true;
    }

    /** Three fictional requests (example.com addresses) so the demo inbox has content. */
    private void addSampleRequests(Club club) {
        Object[][] samples = {
            {"Jordan Lee", "jordan.lee@example.com", "Hi! I've played chess for 3 years and would love to join your Tuesday games.", RequestStatus.DELIVERED, 2},
            {"Priya Patel", "priya.patel@example.com", "Do you teach beginners? I know the rules but have never played in a tournament.", RequestStatus.DELIVERED, 1},
            {"Marcus Green", "marcus.green@example.com", "Is there a membership fee? Also, can transfer students join mid-semester?", RequestStatus.FAILED, 0},
        };
        for (Object[] s : samples) {
            JoinRequest r = new JoinRequest();
            r.setClub(club);
            r.setStudentName((String) s[0]);
            r.setStudentEmail((String) s[1]);
            r.setMessage((String) s[2]);
            r.setStatus((RequestStatus) s[3]);
            r.setDateSubmitted(LocalDateTime.now().minusDays((Integer) s[4]).minusHours(3));
            requests.save(r);
        }
    }

    /** Two upcoming events (FR-14), dated from today so they're never in the past. */
    private void addSampleEvents(Club club) {
        java.time.LocalDate today = java.time.LocalDate.now();
        events.save(event(club, "Blitz tournament", "5-minute games, all levels. Prizes for the top three.",
                today.plusDays(3).atTime(18, 0), today.plusDays(3).atTime(20, 0), "Library Room 210"));
        events.save(event(club, "Beginner lesson night", "Learn openings and basic tactics. Boards provided.",
                today.plusDays(8).atTime(18, 30), today.plusDays(8).atTime(19, 30), "Library Room 210"));
    }

    private static ClubEvent event(Club club, String title, String description,
                                   LocalDateTime start, LocalDateTime end, String location) {
        ClubEvent e = new ClubEvent();
        e.setClub(club);
        e.setTitle(title);
        e.setDescription(description);
        e.setStartsAt(start);
        e.setEndsAt(end);
        e.setLocation(location);
        return e;
    }
}
