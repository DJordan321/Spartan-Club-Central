package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.ClubForm;
import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.EventRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.OfficerRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: rules for finding, showing and managing clubs
 * (FR-01, FR-02, FR-05, FR-08, FR-09, FR-12).
 *
 * THE OWNERSHIP RULE (SPR-03) lives here, in requireOwnedClub(): every
 * leader action on a club (edit, hide, show, delete, read requests) calls it
 * first, using the leader id from the login session. So even if a controller
 * forgets a check, or someone changes the club number in the address bar,
 * a leader can only ever touch their own club.
 */
@Service
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final LeaderRepository leaderRepository;
    private final RequestRepository requestRepository;
    private final OfficerRepository officerRepository;
    private final EventRepository eventRepository;
    private final ClubValidator validator;
    private final AuditLogger audit;
    private final ImageStorage imageStorage;

    public ClubService(ClubRepository clubRepository, CategoryRepository categoryRepository,
                       LeaderRepository leaderRepository, RequestRepository requestRepository,
                       OfficerRepository officerRepository, EventRepository eventRepository,
                       ClubValidator validator, AuditLogger audit, ImageStorage imageStorage) {
        this.clubRepository = clubRepository;
        this.categoryRepository = categoryRepository;
        this.leaderRepository = leaderRepository;
        this.requestRepository = requestRepository;
        this.officerRepository = officerRepository;
        this.eventRepository = eventRepository;
        this.validator = validator;
        this.audit = audit;
        this.imageStorage = imageStorage;
    }

    // ===================== Public (students) =====================

    /**
     * One search path for every keyword/category combination.
     * Blank keyword means "no keyword"; very long input is cut to FieldLimits.SEARCH.
     */
    public List<Club> search(String rawKeyword, Integer categoryId) {
        return clubRepository.search(normalizeKeyword(rawKeyword), categoryId);
    }

    /**
     * A club for its public profile page. Inactive and unknown clubs are
     * treated the same ("not found"), so deactivated listings disappear (FR-12).
     */
    public Club getActiveClub(Long clubId) {
        if (clubId == null) {
            throw new NotFoundException("Club not found");
        }
        return clubRepository.findByClubIdAndActiveTrue(clubId)
                .orElseThrow(() -> new NotFoundException("Club not found"));
    }

    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc();
    }

    /** The category with this id, if it exists (used to label filtered results). */
    public Optional<Category> findCategory(Integer categoryId) {
        return categoryId == null ? Optional.empty() : categoryRepository.findById(categoryId);
    }

    /**
     * Number of active clubs per category id, for the homepage buttons.
     * Counting in Java is fine for ~200 clubs and reuses the tested search query.
     */
    public Map<Integer, Long> activeClubCountsByCategory() {
        return clubRepository.search(null, null).stream()
                .collect(Collectors.groupingBy(c -> c.getCategory().getCategoryId(), Collectors.counting()));
    }

    // ===================== Leaders (dashboard) =====================

    /**
     * The ownership check (SPR-03, SPR-04). Returns the club only if this
     * leader owns it; otherwise records OWNERSHIP_DENIED and throws, which
     * shows the 403 page. Works for active and hidden clubs alike.
     */
    public Club requireOwnedClub(Long leaderId, Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new NotFoundException("Club not found"));
        Long ownedId = leaderRepository.findById(leaderId).map(ClubLeader::getClubId).orElse(null);
        if (!clubId.equals(ownedId)) {
            audit.log("LEADER", leaderId, AuditLogger.OWNERSHIP_DENIED, "CLUB", clubId, false);
            throw new AccessDeniedException("Leader " + leaderId + " does not own club " + clubId);
        }
        return club;
    }

    /** The leader's own club, active or hidden, if they have created one. */
    public Optional<Club> findLeaderClub(Long leaderId) {
        return leaderRepository.findById(leaderId)
                .map(ClubLeader::getClubId)
                .flatMap(clubRepository::findById);
    }

    /** How many join requests a leader's club has received (for the dashboard). */
    public long countRequests(Long leaderId, Long clubId) {
        requireOwnedClub(leaderId, clubId);
        return requestRepository.countByClub_ClubId(clubId);
    }

    /**
     * FR-08: a leader without a club creates one. It is public right away.
     * @throws ValidationException with one message per problem field
     * @throws AccessDeniedException if the leader already has a club (one club per leader)
     */
    @Transactional
    public Club createClub(Long leaderId, ClubForm form) {
        ClubLeader leader = leaderRepository.findById(leaderId).orElseThrow(() -> new NotFoundException("Leader not found"));
        if (leader.getClubId() != null) {
            throw new AccessDeniedException("Leader already has a club");
        }
        Category category = validate(form, null);
        Club club = new Club();
        apply(club, form, category);
        club.setActive(true);
        club = clubRepository.save(club);
        leader.setClub(club);
        leaderRepository.save(leader);
        // FR-13: the leader is listed as the first officer; they can edit or remove it.
        String role = leader.getPosition() == null || leader.getPosition().isBlank() ? "Club leader" : leader.getPosition();
        officerRepository.save(new ClubOfficer(club, leader.getName(), role, 1));
        audit.log("LEADER", leaderId, AuditLogger.CLUB_CREATED, "CLUB", club.getClubId(), true);
        return club;
    }

    /** FR-09: the owning leader edits their club. */
    @Transactional
    public Club updateClub(Long leaderId, Long clubId, ClubForm form) {
        Club club = requireOwnedClub(leaderId, clubId);
        Category category = validate(form, clubId);
        apply(club, form, category);
        club = clubRepository.save(club);
        audit.log("LEADER", leaderId, AuditLogger.CLUB_EDITED, "CLUB", clubId, true);
        return club;
    }

    /** FR-12: hide the club from students (active=false) or show it again (active=true). */
    @Transactional
    public Club setActive(Long leaderId, Long clubId, boolean active) {
        Club club = requireOwnedClub(leaderId, clubId);
        club.setActive(active);
        club = clubRepository.save(club);
        audit.log("LEADER", leaderId, active ? AuditLogger.CLUB_ACTIVATED : AuditLogger.CLUB_DEACTIVATED,
                "CLUB", clubId, true);
        return club;
    }

    /**
     * FR-12: permanently delete the club AND its join requests, officers, events
     * and picture (conflict C5:
     * the confirmation page warns about this). The leader keeps their account
     * and can create a new club afterwards.
     */
    @Transactional
    public void deleteClub(Long leaderId, Long clubId) {
        Club club = requireOwnedClub(leaderId, clubId);
        ClubLeader leader = leaderRepository.findById(leaderId).orElseThrow();
        leader.setClub(null);
        leaderRepository.saveAndFlush(leader);
        requestRepository.deleteByClubId(clubId);
        officerRepository.deleteByClubId(clubId);
        eventRepository.deleteByClubId(clubId);
        String picture = club.getImageFilename();
        clubRepository.delete(club);
        imageStorage.delete(picture);                  // FR-15: the picture file goes too
        audit.log("LEADER", leaderId, AuditLogger.CLUB_DELETED, "CLUB", clubId, true);
    }

    /** Field rules + rules that need the database. Returns the chosen category. */
    private Category validate(ClubForm form, Long ownClubId) {
        Map<String, String> errors = new HashMap<>(validator.validate(form));
        Category category = null;
        if (form.getCategoryId() != null) {
            category = categoryRepository.findById(form.getCategoryId()).orElse(null);
            if (category == null) {
                errors.put("categoryId", "Choose a category from the list.");
            }
        }
        String name = RequestValidator.trim(form.getClubName());
        if (!errors.containsKey("clubName") && clubRepository.findByClubNameIgnoreCase(name)
                .filter(other -> !other.getClubId().equals(ownClubId)).isPresent()) {
            errors.put("clubName", "Another club already uses that name.");
        }
        if (!errors.isEmpty()) {
            // Keep the form's field order for the error summary.
            Map<String, String> ordered = new LinkedHashMap<>();
            for (String f : List.of("clubName", "description", "categoryId", "meetingTime", "contactEmail",
                    "contactInfo", "instagramUrl", "websiteUrl", "otherSocialUrl")) {
                if (errors.containsKey(f)) {
                    ordered.put(f, errors.get(f));
                }
            }
            throw new ValidationException(ordered);
        }
        return category;
    }

    private static void apply(Club club, ClubForm form, Category category) {
        club.setClubName(RequestValidator.trim(form.getClubName()));
        club.setDescription(RequestValidator.trim(form.getDescription()));
        club.setCategory(category);
        club.setMeetingTime(blankToNull(form.getMeetingTime()));
        club.setContactEmail(RequestValidator.trim(form.getContactEmail()));
        club.setContactInfo(blankToNull(form.getContactInfo()));
        club.setInstagramUrl(blankToNull(form.getInstagramUrl()));
        club.setWebsiteUrl(blankToNull(form.getWebsiteUrl()));
        club.setOtherSocialUrl(blankToNull(form.getOtherSocialUrl()));
    }

    private static String blankToNull(String value) {
        String v = RequestValidator.trim(value);
        return v.isEmpty() ? null : v;
    }

    /** Trims the keyword; returns null when there is nothing to search for. */
    static String normalizeKeyword(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return null;
        }
        String kw = rawKeyword.trim();
        return kw.length() > FieldLimits.SEARCH ? kw.substring(0, FieldLimits.SEARCH) : kw;
    }
}
