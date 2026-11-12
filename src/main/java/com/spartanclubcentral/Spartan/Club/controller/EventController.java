package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.EventForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.EventService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller layer: the leader's club events (FR-14), ROLE_LEADER only.
 *
 *   GET  /dashboard/clubs/{id}/events                     upcoming + past events
 *   GET  /dashboard/clubs/{id}/events/new                 create form
 *   POST /dashboard/clubs/{id}/events                     create
 *   GET  /dashboard/clubs/{id}/events/{eid}/edit          edit form
 *   POST /dashboard/clubs/{id}/events/{eid}               save
 *   GET  /dashboard/clubs/{id}/events/{eid}/delete        "are you sure?"
 *   POST /dashboard/clubs/{id}/events/{eid}/delete        delete
 */
@Controller
public class EventController {

    private final EventService eventService;
    private final ClubService clubService;

    public EventController(EventService eventService, ClubService clubService) {
        this.eventService = eventService;
        this.clubService = clubService;
    }

    @GetMapping("/dashboard/clubs/{id}/events")
    public String list(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        model.addAttribute("upcoming", eventService.upcomingForLeader(me.getId(), id));
        model.addAttribute("past", eventService.pastForLeader(me.getId(), id));
        return "dashboard/events";
    }

    @GetMapping("/dashboard/clubs/{id}/events/new")
    public String newEvent(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        EventForm form = new EventForm();
        form.setDate(LocalDate.now().plusDays(7).toString());
        return showForm(model, clubService.requireOwnedClub(me.getId(), id), null, form, Map.of());
    }

    @PostMapping("/dashboard/clubs/{id}/events")
    public String create(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                         @ModelAttribute("form") EventForm form, Model model) {
        try {
            eventService.create(me.getId(), id, form);
            return "redirect:/dashboard/clubs/" + id + "/events?created";
        } catch (ValidationException e) {
            return showForm(model, clubService.requireOwnedClub(me.getId(), id), null, form, e.getFieldErrors());
        }
    }

    @GetMapping("/dashboard/clubs/{id}/events/{eid}/edit")
    public String edit(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                       @PathVariable("eid") Long eid, Model model) {
        var event = eventService.getForLeader(me.getId(), id, eid);
        return showForm(model, clubService.requireOwnedClub(me.getId(), id), eid, EventForm.from(event), Map.of());
    }

    @PostMapping("/dashboard/clubs/{id}/events/{eid}")
    public String update(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                         @PathVariable("eid") Long eid, @ModelAttribute("form") EventForm form, Model model) {
        try {
            eventService.update(me.getId(), id, eid, form);
            return "redirect:/dashboard/clubs/" + id + "/events?saved";
        } catch (ValidationException e) {
            return showForm(model, clubService.requireOwnedClub(me.getId(), id), eid, form, e.getFieldErrors());
        }
    }

    @GetMapping("/dashboard/clubs/{id}/events/{eid}/delete")
    public String confirmDelete(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                                @PathVariable("eid") Long eid, Model model) {
        model.addAttribute("event", eventService.getForLeader(me.getId(), id, eid));
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        return "dashboard/confirm-delete-event";
    }

    @PostMapping("/dashboard/clubs/{id}/events/{eid}/delete")
    public String delete(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, @PathVariable("eid") Long eid) {
        eventService.delete(me.getId(), id, eid);
        return "redirect:/dashboard/clubs/" + id + "/events?deleted";
    }

    private String showForm(Model model, Club club, Long eventId, EventForm form, Map<String, String> errors) {
        model.addAttribute("club", club);
        model.addAttribute("eventId", eventId);              // null when creating
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        return "dashboard/event-form";
    }
}
