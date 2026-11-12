package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.OfficerForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;
import com.spartanclubcentral.Spartan.Club.repository.OfficerRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: the officers listed on a club's page (FR-13).
 *
 * Every leader action first calls ClubService.requireOwnedClub (SPR-03), and
 * then checks the officer really belongs to THAT club, so an officer number
 * from another club can't be edited through your own club's address.
 */
@Service
@Transactional(readOnly = true)
public class OfficerService {

    private final OfficerRepository officers;
    private final ClubService clubService;
    private final OfficerValidator validator;
    private final AuditLogger audit;

    public OfficerService(OfficerRepository officers, ClubService clubService, OfficerValidator validator, AuditLogger audit) {
        this.officers = officers;
        this.clubService = clubService;
        this.validator = validator;
        this.audit = audit;
    }

    /** For the public club page: only for active clubs (hidden clubs are 404). */
    public List<ClubOfficer> listForPublicClub(Long clubId) {
        clubService.getActiveClub(clubId);
        return officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(clubId);
    }

    /** For the owning leader's dashboard (active or hidden club). */
    public List<ClubOfficer> listForLeader(Long leaderId, Long clubId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(clubId);
    }

    public ClubOfficer getForLeader(Long leaderId, Long clubId, Long officerId) {
        clubService.requireOwnedClub(leaderId, clubId);
        return requireOfficerOfClub(leaderId, clubId, officerId);
    }

    @Transactional
    public ClubOfficer add(Long leaderId, Long clubId, OfficerForm form) {
        Club club = clubService.requireOwnedClub(leaderId, clubId);
        List<ClubOfficer> current = officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(clubId);
        if (current.size() >= FieldLimits.MAX_OFFICERS) {
            throw new ValidationException(Map.of("name",
                    "A club can list up to " + FieldLimits.MAX_OFFICERS + " officers. Remove one first."));
        }
        check(form);
        int next = current.stream().mapToInt(ClubOfficer::getDisplayOrder).max().orElse(0) + 1;
        ClubOfficer officer = officers.save(new ClubOfficer(club, RequestValidator.trim(form.getName()),
                RequestValidator.trim(form.getRole()), next));
        audited(leaderId, clubId);
        return officer;
    }

    @Transactional
    public ClubOfficer update(Long leaderId, Long clubId, Long officerId, OfficerForm form) {
        clubService.requireOwnedClub(leaderId, clubId);
        ClubOfficer officer = requireOfficerOfClub(leaderId, clubId, officerId);
        check(form);
        officer.setName(RequestValidator.trim(form.getName()));
        officer.setRole(RequestValidator.trim(form.getRole()));
        officer = officers.save(officer);
        audited(leaderId, clubId);
        return officer;
    }

    @Transactional
    public void remove(Long leaderId, Long clubId, Long officerId) {
        clubService.requireOwnedClub(leaderId, clubId);
        ClubOfficer officer = requireOfficerOfClub(leaderId, clubId, officerId);
        officers.delete(officer);
        officers.flush();
        renumber(clubId);
        audited(leaderId, clubId);
    }

    /** Moves an officer one place up (-1) or down (+1) in the list. */
    @Transactional
    public void move(Long leaderId, Long clubId, Long officerId, int direction) {
        clubService.requireOwnedClub(leaderId, clubId);
        requireOfficerOfClub(leaderId, clubId, officerId);
        List<ClubOfficer> list = new ArrayList<>(officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(clubId));
        int i = indexOf(list, officerId);
        int j = i + Integer.signum(direction);
        if (j < 0 || j >= list.size()) {
            return;                                    // already at the top/bottom
        }
        list.add(j, list.remove(i));
        for (int k = 0; k < list.size(); k++) {
            list.get(k).setDisplayOrder(k + 1);
        }
        officers.saveAll(list);
        audited(leaderId, clubId);
    }

    private ClubOfficer requireOfficerOfClub(Long leaderId, Long clubId, Long officerId) {
        ClubOfficer officer = officers.findById(officerId).orElseThrow(() -> new NotFoundException("Officer not found"));
        if (!clubId.equals(officer.getClubId())) {
            audit.log("LEADER", leaderId, AuditLogger.OWNERSHIP_DENIED, "CLUB", officer.getClubId(), false);
            throw new AccessDeniedException("Officer " + officerId + " is not in club " + clubId);
        }
        return officer;
    }

    private void renumber(Long clubId) {
        List<ClubOfficer> list = officers.findByClub_ClubIdOrderByDisplayOrderAscOfficerIdAsc(clubId);
        for (int k = 0; k < list.size(); k++) {
            list.get(k).setDisplayOrder(k + 1);
        }
        officers.saveAll(list);
    }

    private void check(OfficerForm form) {
        Map<String, String> errors = validator.validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void audited(Long leaderId, Long clubId) {
        audit.log("LEADER", leaderId, AuditLogger.OFFICERS_UPDATED, "CLUB", clubId, true);
    }

    private static int indexOf(List<ClubOfficer> list, Long officerId) {
        for (int k = 0; k < list.size(); k++) {
            if (list.get(k).getOfficerId().equals(officerId)) {
                return k;
            }
        }
        throw new NotFoundException("Officer not found");
    }
}
