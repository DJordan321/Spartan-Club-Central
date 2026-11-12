package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.EventService;
import com.spartanclubcentral.Spartan.Club.service.OfficerService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller layer: the public student pages (no login).
 *
 *   GET /             homepage: search box + one button per category (NFR-03)
 *   GET /clubs        directory: keyword + category search (FR-01, FR-02)
 *   GET /clubs/{id}   club profile with the join form (FR-05, FR-06, FR-03)
 *
 * Only passes data between ClubService and the templates; no business rules.
 * Never puts join request data in the model, so student contact details
 * can't appear on public pages (NFR-02).
 */
@Controller
public class ClubController {

    private final ClubService clubService;
    private final OfficerService officerService;
    private final EventService eventService;

    public ClubController(ClubService clubService, OfficerService officerService, EventService eventService) {
        this.clubService = clubService;
        this.officerService = officerService;
        this.eventService = eventService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", clubService.listCategories());
        model.addAttribute("counts", clubService.activeClubCountsByCategory());
        model.addAttribute("comingUp", eventService.comingUpThisWeek(6));          // FR-14
        return "index";
    }

    @GetMapping("/clubs")
    public String directory(@RequestParam(name = "q", required = false) String keyword,
                            @RequestParam(name = "category", required = false) Integer categoryId,
                            Model model) {
        List<Club> clubs = clubService.search(keyword, categoryId);
        model.addAttribute("clubs", clubs);
        model.addAttribute("categories", clubService.listCategories());
        model.addAttribute("q", keyword == null ? "" : keyword.trim());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("selectedCategoryName",
                clubService.findCategory(categoryId).map(Category::getCategoryName).orElse(null));
        return "directory";
    }

    @GetMapping("/clubs/{id}")
    public String profile(@PathVariable("id") Long id, Model model) {
        model.addAttribute("club", clubService.getActiveClub(id));
        model.addAttribute("officers", officerService.listForPublicClub(id));   // FR-13
        model.addAttribute("events", eventService.upcomingForPublicClub(id));    // FR-14
        model.addAttribute("joinForm", new JoinRequestForm());   // empty join form (FR-03)
        model.addAttribute("errors", Map.of());
        return "club";
    }
}
