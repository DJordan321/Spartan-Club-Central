package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.ClubForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.EventService;
import com.spartanclubcentral.Spartan.Club.service.OfficerService;
import com.spartanclubcentral.Spartan.Club.service.RequestService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller layer: the club leader's dashboard (ROLE_LEADER only, SPR-01).
 *
 *   GET  /dashboard                          overview: your club, status, request count
 *   GET  /dashboard/clubs/new                create form (only if you have no club)   FR-08
 *   POST /dashboard/clubs                    create
 *   GET  /dashboard/clubs/{id}/edit          edit form                                FR-09
 *   POST /dashboard/clubs/{id}               save changes
 *   POST /dashboard/clubs/{id}/deactivate    hide from students                        FR-12
 *   POST /dashboard/clubs/{id}/activate      show again
 *   GET  /dashboard/clubs/{id}/delete        "are you sure?" page
 *   POST /dashboard/clubs/{id}/delete        delete club and its requests             FR-12
 *   GET  /dashboard/clubs/{id}/requests      request inbox                            NFR-02
 *
 * The club id stays in the address on purpose, so the SPR-03 test (change the
 * number in the address bar) can be run. The leader is always "me" from the
 * login session, and every club action goes through ClubService's ownership
 * check, so another leader's club only ever gives the 403 page.
 * Every change is a POST with a CSRF token.
 */
@Controller
public class DashboardController {

    private final AuthService authService;
    private final ClubService clubService;
    private final RequestService requestService;
    private final OfficerService officerService;
    private final EventService eventService;

    public DashboardController(AuthService authService, ClubService clubService, RequestService requestService,
                               OfficerService officerService, EventService eventService) {
        this.authService = authService;
        this.clubService = clubService;
        this.requestService = requestService;
        this.officerService = officerService;
        this.eventService = eventService;
    }

    @GetMapping("/dashboard")
    public String home(@AuthenticationPrincipal AccountPrincipal me, Model model) {
        model.addAttribute("leader", authService.getLeader(me.getId()));
        Optional<Club> club = clubService.findLeaderClub(me.getId());
        model.addAttribute("club", club.orElse(null));
        model.addAttribute("requestCount", club.map(c -> clubService.countRequests(me.getId(), c.getClubId())).orElse(0L));
        model.addAttribute("officers", club.map(c -> officerService.listForLeader(me.getId(), c.getClubId())).orElse(List.of()));
        model.addAttribute("events", club.map(c -> eventService.upcomingForLeader(me.getId(), c.getClubId())).orElse(List.of()));
        return "dashboard/home";
    }

    // ---------- Create (FR-08) ----------

    @GetMapping("/dashboard/clubs/new")
    public String newClub(@AuthenticationPrincipal AccountPrincipal me, Model model) {
        if (clubService.findLeaderClub(me.getId()).isPresent()) {
            return "redirect:/dashboard";      // one club per leader
        }
        return showForm(model, new ClubForm(), Map.of(), null);
    }

    @PostMapping("/dashboard/clubs")
    public String create(@AuthenticationPrincipal AccountPrincipal me, @ModelAttribute("form") ClubForm form, Model model) {
        try {
            clubService.createClub(me.getId(), form);
            return "redirect:/dashboard?created";
        } catch (ValidationException e) {
            return showForm(model, form, e.getFieldErrors(), null);
        }
    }

    // ---------- Edit (FR-09) ----------

    @GetMapping("/dashboard/clubs/{id}/edit")
    public String edit(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        Club club = clubService.requireOwnedClub(me.getId(), id);
        return showForm(model, ClubForm.from(club), Map.of(), club);
    }

    @PostMapping("/dashboard/clubs/{id}")
    public String update(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                         @ModelAttribute("form") ClubForm form, Model model) {
        try {
            clubService.updateClub(me.getId(), id, form);
            return "redirect:/dashboard?saved";
        } catch (ValidationException e) {
            return showForm(model, form, e.getFieldErrors(), clubService.requireOwnedClub(me.getId(), id));
        }
    }

    // ---------- Hide / show (FR-12) ----------

    @PostMapping("/dashboard/clubs/{id}/deactivate")
    public String deactivate(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        clubService.setActive(me.getId(), id, false);
        return "redirect:/dashboard?deactivated";
    }

    @PostMapping("/dashboard/clubs/{id}/activate")
    public String activate(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        clubService.setActive(me.getId(), id, true);
        return "redirect:/dashboard?activated";
    }

    // ---------- Delete (FR-12) ----------

    @GetMapping("/dashboard/clubs/{id}/delete")
    public String confirmDelete(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        model.addAttribute("requestCount", clubService.countRequests(me.getId(), id));
        return "dashboard/confirm-delete";
    }

    @PostMapping("/dashboard/clubs/{id}/delete")
    public String delete(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        clubService.deleteClub(me.getId(), id);
        return "redirect:/dashboard?deleted";
    }

    // ---------- Request inbox (NFR-02, SPR-04) ----------

    @GetMapping("/dashboard/clubs/{id}/requests")
    public String requests(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        model.addAttribute("requests", requestService.getRequestsForClub(me.getId(), id));
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        return "dashboard/requests";
    }

    private String showForm(Model model, ClubForm form, Map<String, String> errors, Club existing) {
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        model.addAttribute("club", existing);          // null when creating
        model.addAttribute("categories", clubService.listCategories());
        return "dashboard/club-form";
    }
}
