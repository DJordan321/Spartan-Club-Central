package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.EventService;
import com.spartanclubcentral.Spartan.Club.service.OfficerService;
import com.spartanclubcentral.Spartan.Club.service.RequestService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller layer: the public join/contact request form (FR-03). No login.
 *
 *   POST /clubs/{id}/requests        submit the form (CSRF token required)
 *   GET  /clubs/{id}/requests/sent   confirmation page
 *
 * Uses Post/Redirect/Get: after a successful submit the browser is sent to
 * the confirmation page, so refreshing it can't send the request twice.
 * All rules live in RequestService; this class only picks the page to show.
 */
@Controller
public class RequestController {

    private final RequestService requestService;
    private final ClubService clubService;
    private final OfficerService officerService;
    private final EventService eventService;

    public RequestController(RequestService requestService, ClubService clubService, OfficerService officerService,
                             EventService eventService) {
        this.requestService = requestService;
        this.clubService = clubService;
        this.officerService = officerService;
        this.eventService = eventService;
    }

    @PostMapping("/clubs/{id}/requests")
    public String submit(@PathVariable("id") Long id,
                         @ModelAttribute("joinForm") JoinRequestForm form,
                         Model model) {
        try {
            requestService.submit(id, form);
            return "redirect:/clubs/" + id + "/requests/sent";
        } catch (ValidationException e) {
            // Show the club page again with the typed values and one message per field.
            model.addAttribute("club", clubService.getActiveClub(id));
            model.addAttribute("officers", officerService.listForPublicClub(id));
            model.addAttribute("events", eventService.upcomingForPublicClub(id));
            model.addAttribute("errors", e.getFieldErrors());
            return "club";
        }
    }

    @GetMapping("/clubs/{id}/requests/sent")
    public String sent(@PathVariable("id") Long id, Model model) {
        model.addAttribute("club", clubService.getActiveClub(id));
        return "request-sent";
    }
}
