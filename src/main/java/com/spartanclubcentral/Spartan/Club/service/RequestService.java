package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.model.RequestStatus;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

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
 * Never reads the logged-in user: students submit without an account.
 * Phase 5 adds getRequestsForClub() with the leader ownership check (SPR-04).
 */
@Service
public class RequestService {

    private final RequestValidator validator;
    private final ClubService clubService;
    private final RequestRepository requestRepository;
    private final NotificationService notificationService;

    public RequestService(RequestValidator validator, ClubService clubService,
                          RequestRepository requestRepository, NotificationService notificationService) {
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
}
