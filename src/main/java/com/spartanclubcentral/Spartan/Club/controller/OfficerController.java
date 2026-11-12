package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.OfficerForm;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import com.spartanclubcentral.Spartan.Club.service.OfficerService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller layer: the leader's officer list (FR-13), ROLE_LEADER only.
 *
 *   GET  /dashboard/clubs/{id}/officers                       list + add form
 *   POST /dashboard/clubs/{id}/officers                       add
 *   GET  /dashboard/clubs/{id}/officers/{oid}/edit            edit form
 *   POST /dashboard/clubs/{id}/officers/{oid}                 save
 *   POST /dashboard/clubs/{id}/officers/{oid}/move-up|down    reorder
 *   GET  /dashboard/clubs/{id}/officers/{oid}/remove          "are you sure?"
 *   POST /dashboard/clubs/{id}/officers/{oid}/remove          remove
 *
 * OfficerService checks club ownership and that the officer belongs to that club.
 */
@Controller
public class OfficerController {

    private final OfficerService officerService;
    private final ClubService clubService;

    public OfficerController(OfficerService officerService, ClubService clubService) {
        this.officerService = officerService;
        this.clubService = clubService;
    }

    @GetMapping("/dashboard/clubs/{id}/officers")
    public String list(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        return showList(me, id, model, new OfficerForm(), Map.of());
    }

    @PostMapping("/dashboard/clubs/{id}/officers")
    public String add(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                      @ModelAttribute("form") OfficerForm form, Model model) {
        try {
            officerService.add(me.getId(), id, form);
            return "redirect:/dashboard/clubs/" + id + "/officers?added";
        } catch (ValidationException e) {
            return showList(me, id, model, form, e.getFieldErrors());
        }
    }

    @GetMapping("/dashboard/clubs/{id}/officers/{oid}/edit")
    public String edit(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                       @PathVariable("oid") Long oid, Model model) {
        model.addAttribute("officer", officerService.getForLeader(me.getId(), id, oid));
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        model.addAttribute("form", OfficerForm.from(officerService.getForLeader(me.getId(), id, oid)));
        model.addAttribute("errors", Map.of());
        return "dashboard/officer-edit";
    }

    @PostMapping("/dashboard/clubs/{id}/officers/{oid}")
    public String update(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                         @PathVariable("oid") Long oid, @ModelAttribute("form") OfficerForm form, Model model) {
        try {
            officerService.update(me.getId(), id, oid, form);
            return "redirect:/dashboard/clubs/" + id + "/officers?saved";
        } catch (ValidationException e) {
            model.addAttribute("officer", officerService.getForLeader(me.getId(), id, oid));
            model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
            model.addAttribute("errors", e.getFieldErrors());
            return "dashboard/officer-edit";
        }
    }

    @PostMapping("/dashboard/clubs/{id}/officers/{oid}/move-up")
    public String moveUp(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, @PathVariable("oid") Long oid) {
        officerService.move(me.getId(), id, oid, -1);
        return "redirect:/dashboard/clubs/" + id + "/officers?moved";
    }

    @PostMapping("/dashboard/clubs/{id}/officers/{oid}/move-down")
    public String moveDown(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, @PathVariable("oid") Long oid) {
        officerService.move(me.getId(), id, oid, 1);
        return "redirect:/dashboard/clubs/" + id + "/officers?moved";
    }

    @GetMapping("/dashboard/clubs/{id}/officers/{oid}/remove")
    public String confirmRemove(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                                @PathVariable("oid") Long oid, Model model) {
        model.addAttribute("officer", officerService.getForLeader(me.getId(), id, oid));
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        return "dashboard/confirm-remove-officer";
    }

    @PostMapping("/dashboard/clubs/{id}/officers/{oid}/remove")
    public String remove(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, @PathVariable("oid") Long oid) {
        officerService.remove(me.getId(), id, oid);
        return "redirect:/dashboard/clubs/" + id + "/officers?removed";
    }

    private String showList(AccountPrincipal me, Long id, Model model, OfficerForm form, Map<String, String> errors) {
        model.addAttribute("officers", officerService.listForLeader(me.getId(), id));
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        model.addAttribute("maxOfficers", FieldLimits.MAX_OFFICERS);
        return "dashboard/officers";
    }
}
