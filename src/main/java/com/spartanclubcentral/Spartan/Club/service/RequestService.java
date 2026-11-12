package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.model.RequestStatus;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: join/contact requests from students (FR-03, FR-07).
 *
 * Order matters (fixes TC-07):
 *   1. validate        -> invalid input saves nothing and sends nothing
 *   2. save SUBMITTED  -> the request is safely stored before any email
 *   3. email the club  -> NotificationService never throws
 *   4. save DELIVERED or FAILED
 *
 * Deliberately NOT @Transactional as a whole: each save commits on its own,
 * so an email problem can never roll back the stored request.
 * Trade-off: the student waits for the email (about a second, at most the
 * 3-second timeout), which is simpler than a background queue at this scale.
 *
 * submit() never reads the logged-in user: students submit without an account.
 * getRequestsForClub() is the leader inbox: ownership is checked first (SPR-04).
 */
@Service
public class RequestService {

    private final RequestValidator validator;
    private final ClubService clubService;
    private final RequestRepository requestRepository;
    private final AuditLogger audit;
    private final NotificationService notificationService;

    public RequestService(RequestValidator validator, ClubService clubService,
                          RequestRepository requestRepository, NotificationService notificationService,
                          AuditLogger audit) {
        this.audit = audit;
        this.validator = validator;
        this.clubService = clubService;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
    }

    /**
     * Validates, stores and emails a join request.
     *
     * @throws ValidationException if any field is invalid (nothing saved or sent)
     * @throws com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException
     *         if the club is unknown or inactive
     */
    public JoinRequest submit(Long clubId, JoinRequestForm form) {
        Map<String, String> errors = validator.validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Club club = clubService.getActiveClub(clubId);

        JoinRequest request = new JoinRequest();
        request.setClub(club);
        request.setStudentName(RequestValidator.trim(form.getName()));
        request.setStudentEmail(RequestValidator.trim(form.getEmail()));
        request.setMessage(RequestValidator.trim(form.getMessage()));
        request.setStatus(RequestStatus.SUBMITTED);
        request.setDateSubmitted(LocalDateTime.now());
        request = requestRepository.save(request);

        boolean sent = notificationService.notifyClub(club, request);
        request.setStatus(sent ? RequestStatus.DELIVERED : RequestStatus.FAILED);
        return requestRepository.save(request);
    }

    /**
     * The leader's request inbox (NFR-02, SPR-04): newest first, and ONLY for
     * a club this leader owns. The ownership check runs before any student
     * data is read, so changing the club number in the address bar shows
     * nothing but the 403 page.
     */
    public List<JoinRequest> getRequestsForClub(Long leaderId, Long clubId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return requestRepository.findByClub_ClubIdOrderByDateSubmittedDesc(clubId);
    }

    /**
     * New (unseen) requests for the leader's own club: the number on the
     * dashboard's "Join requests" button.
     */
    public long countNewRequests(Long leaderId, Long clubId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return requestRepository.countByClub_ClubIdAndSeenFalse(clubId);
    }

    /**
     * Ticks a request off as seen, or back to new. Same two checks as officers
     * and events: the leader owns the club, AND the request was sent to that
     * club, so a request number from another club can't be changed through
     * your own club's address (SPR-03, SPR-04).
     */
    @Transactional
    public void setSeen(Long leaderId, Long clubId, Long requestId, boolean seen) {
        clubService.requireOwnedClub(leaderId, clubId);
        JoinRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        Long requestClubId = request.getClub().getClubId();
        if (!clubId.equals(requestClubId)) {
            audit.log("LEADER", leaderId, AuditLogger.OWNERSHIP_DENIED, "CLUB", requestClubId, false);
            throw new AccessDeniedException("Request " + requestId + " is not in club " + clubId);
        }
        request.setSeen(seen);
        requestRepository.save(request);
    }
}
