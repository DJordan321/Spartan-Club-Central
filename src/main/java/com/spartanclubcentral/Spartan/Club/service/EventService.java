package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.EventForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubEvent;
import com.spartanclubcentral.Spartan.Club.repository.EventRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: club events (FR-14).
 *
 * Public lists show only upcoming events of ACTIVE clubs. Every leader action
 * calls ClubService.requireOwnedClub first (SPR-03) and then checks the event
 * belongs to that club, so another club's event number can't be used through
 * your own club's address.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    /** How many upcoming events a club page shows. */
    public static final int PUBLIC_LIMIT = 5;

    private final EventRepository events;
    private final ClubService clubService;
    private final EventValidator validator;
    private final AuditLogger audit;

    public EventService(EventRepository events, ClubService clubService, EventValidator validator, AuditLogger audit) {
        this.events = events;
        this.clubService = clubService;
        this.validator = validator;
        this.audit = audit;
    }

    /** The next few events for a public club page (hidden clubs are 404). */
    public List<ClubEvent> upcomingForPublicClub(Long clubId) {
        clubService.getActiveClub(clubId);
        return events.findUpcoming(clubId, LocalDateTime.now()).stream().limit(PUBLIC_LIMIT).toList();
    }

    /** Homepage "Coming up this week": upcoming events of active clubs in the next 7 days. */
    public List<ClubEvent> comingUpThisWeek(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return events.findUpcomingForActiveClubs(now, now.plusDays(7)).stream().limit(limit).toList();
    }

    public List<ClubEvent> upcomingForLeader(Long leaderId, Long clubId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return events.findUpcoming(clubId, LocalDateTime.now());
    }

    public List<ClubEvent> pastForLeader(Long leaderId, Long clubId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return events.findPast(clubId, LocalDateTime.now());
    }

    public ClubEvent getForLeader(Long leaderId, Long clubId, Long eventId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return requireEventOfClub(leaderId, clubId, eventId);
    }

    @Transactional
    public ClubEvent create(Long leaderId, Long clubId, EventForm form) {
        Club club = clubService.requireOwnedClub(leaderId, clubId);
        ClubEvent event = new ClubEvent();
        event.setClub(club);
        apply(event, form, true);
        event = events.save(event);
        audit.log("LEADER", leaderId, AuditLogger.EVENT_CREATED, "CLUB", clubId, true);
        return event;
    }

    @Transactional
    public ClubEvent update(Long leaderId, Long clubId, Long eventId, EventForm form) {
        clubService.requireOwnedClub(leaderId, clubId);
        ClubEvent event = requireEventOfClub(leaderId, clubId, eventId);
        apply(event, form, false);
        event = events.save(event);
        audit.log("LEADER", leaderId, AuditLogger.EVENT_EDITED, "CLUB", clubId, true);
        return event;
    }

    @Transactional
    public void delete(Long leaderId, Long clubId, Long eventId) {
        clubService.requireOwnedClub(leaderId, clubId);
        events.delete(requireEventOfClub(leaderId, clubId, eventId));
        audit.log("LEADER", leaderId, AuditLogger.EVENT_DELETED, "CLUB", clubId, true);
    }

    private void apply(ClubEvent event, EventForm form, boolean isNew) {
        Map<String, String> errors = validator.validate(form, isNew, LocalDateTime.now());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        EventValidator.Times times = validator.times(form);
        event.setTitle(RequestValidator.trim(form.getTitle()));
        event.setStartsAt(times.start());
        event.setEndsAt(times.end());
        String description = RequestValidator.trim(form.getDescription());
        event.setDescription(description.isEmpty() ? null : description);
        String location = RequestValidator.trim(form.getLocation());
        event.setLocation(location.isEmpty() ? null : location);
    }

    private ClubEvent requireEventOfClub(Long leaderId, Long clubId, Long eventId) {
        ClubEvent event = events.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!clubId.equals(event.getClubId())) {
            audit.log("LEADER", leaderId, AuditLogger.OWNERSHIP_DENIED, "CLUB", event.getClubId(), false);
            throw new AccessDeniedException("Event " + eventId + " is not in club " + clubId);
        }
        return event;
    }
}
