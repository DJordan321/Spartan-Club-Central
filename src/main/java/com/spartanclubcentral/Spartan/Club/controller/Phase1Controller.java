package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.SystemStatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller layer: TEMPORARY Phase 1 checkpoint page.
 *
 * Shows that the database, entities, sample data and search query work,
 * before the real student pages exist. Phase 2 replaces "/" with the real
 * homepage and deletes this class and templates/phase1/.
 * Like every controller it only passes data between services and the page.
 */
@Controller
public class Phase1Controller {

    private final ClubService clubService;
    private final SystemStatusService statusService;

    public Phase1Controller(ClubService clubService, SystemStatusService statusService) {
        this.clubService = clubService;
        this.statusService = statusService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/phase-1";
    }

    @GetMapping("/phase-1")
    public String checkpoint(@RequestParam(name = "q", required = false) String keyword,
                             @RequestParam(name = "category", required = false) Integer categoryId,
                             Model model) {
        SystemStatusService.Status status = statusService.currentStatus();
        model.addAttribute("status", status);
        model.addAttribute("q", keyword == null ? "" : keyword);
        model.addAttribute("selectedCategory", categoryId);
        if (status.connected()) {
            model.addAttribute("categories", clubService.listCategories());
            model.addAttribute("clubs", clubService.search(keyword, categoryId));
        }
        return "phase1/status";
    }
}
