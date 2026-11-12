package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.model.AuditLog;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: the administrator's tools (account recovery and
 * deactivations, FR-11, FR-12; conflict C7).
 *
 * The administrator sees every club (including hidden ones) and every leader
 * account, can hide/show any club, disable/enable a leader account (for
 * example a fake one), and send a leader a password reset link. The admin
 * never sees the password and never sees students' join requests, only how
 * many a club has (NFR-02).
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    /** One row in the admin's club list. */
    public record ClubRow(Club club, ClubLeader leader, long requestCount) { }

    /**
     * One row in the admin's leader list. The club name is looked up here,
     * inside the database transaction: the page can't load it later because
     * the leader's club is fetched lazily and the session is closed by then.
     */
    public record LeaderRow(ClubLeader leader, String clubName) { }

    private final ClubRepository clubs;
    private final LeaderRepository leaders;
    private final RequestRepository requests;
    private final AuditLogRepository auditLogs;
    private final AuthService authService;
    private final AuditLogger audit;

    public AdminService(ClubRepository clubs, LeaderRepository leaders, RequestRepository requests,
                        AuditLogRepository auditLogs, AuthService authService, AuditLogger audit) {
        this.clubs = clubs;
        this.leaders = leaders;
        this.requests = requests;
        this.auditLogs = auditLogs;
        this.authService = authService;
        this.audit = audit;
    }

    public List<ClubRow> listClubs() {
        Map<Long, ClubLeader> byClub = leaders.findAll().stream()
                .filter(l -> l.getClubId() != null)
                .collect(Collectors.toMap(ClubLeader::getClubId, Function.identity()));
        return clubs.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getClubName().toLowerCase()))
                .map(c -> new ClubRow(c, byClub.get(c.getClubId()), requests.countByClub_ClubId(c.getClubId())))
                .toList();
    }

    public List<LeaderRow> listLeaders() {
        return leaders.findAll().stream()
                .sorted(Comparator.comparing(l -> l.getName().toLowerCase()))
                .map(l -> new LeaderRow(l, l.getClub() == null ? null : l.getClub().getClubName()))
                .toList();
    }

    /** The 50 most recent security events, newest first (SPR-08). */
    public List<AuditLog> recentAudit() {
        return auditLogs.findTop50ByOrderByEventTimeDesc().stream()
                .sorted(Comparator.comparing(AuditLog::getLogId).reversed())
                .toList();
    }

    /** FR-12: hide or show any club. */
    @Transactional
    public Club setClubActive(Long adminId, Long clubId, boolean active) {
        Club club = clubs.findById(clubId).orElseThrow(() -> new NotFoundException("Club not found"));
        club.setActive(active);
        club = clubs.save(club);
        audit.log("ADMIN", adminId, active ? AuditLogger.CLUB_ACTIVATED : AuditLogger.CLUB_DEACTIVATED, "CLUB", clubId, true);
        return club;
    }

    /** Conflict C7: block (or unblock) a leader account, e.g. a fake one. A disabled account can't log in or reset its password. */
    @Transactional
    public ClubLeader setLeaderEnabled(Long adminId, Long leaderId, boolean enabled) {
        ClubLeader leader = leaders.findById(leaderId).orElseThrow(() -> new NotFoundException("Leader not found"));
        leader.setEnabled(enabled);
        leader = leaders.save(leader);
        audit.log("ADMIN", adminId, enabled ? AuditLogger.LEADER_ENABLED : AuditLogger.LEADER_DISABLED, "LEADER", leaderId, true);
        return leader;
    }

    /**
     * FR-11 (account recovery): emails the leader a reset link. The admin
     * never sees or sets the password.
     *
     * @return true if the email was accepted by the mail server
     */
    @Transactional
    public boolean sendResetLink(Long adminId, Long leaderId) {
        ClubLeader leader = leaders.findById(leaderId).orElseThrow(() -> new NotFoundException("Leader not found"));
        boolean sent = authService.sendResetLink(leader);
        audit.log("ADMIN", adminId, AuditLogger.RESET_LINK_SENT, "LEADER", leaderId, sent);
        return sent;
    }
}
