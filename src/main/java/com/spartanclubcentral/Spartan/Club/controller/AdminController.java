package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.AdminService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller layer: the administrator area (ROLE_ADMIN only, enforced by SecurityConfig).
 *
 *   GET  /admin                              clubs, leaders, recent audit log
 *   POST /admin/clubs/{id}/deactivate        hide any club          FR-12
 *   POST /admin/clubs/{id}/activate          show it again
 *   POST /admin/leaders/{id}/send-reset      email a reset link     FR-11
 *   POST /admin/leaders/{id}/disable         block a leader account (conflict C7)
 *   POST /admin/leaders/{id}/enable          unblock it
 */
@Controller
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin")
    public String home(Model model) {
        model.addAttribute("clubs", adminService.listClubs());
        model.addAttribute("leaders", adminService.listLeaders());
        model.addAttribute("auditLog", adminService.recentAudit());
        return "admin/home";
    }

    @PostMapping("/admin/clubs/{id}/deactivate")
    public String deactivateClub(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        adminService.setClubActive(me.getId(), id, false);
        return "redirect:/admin?clubHidden#clubs";
    }

    @PostMapping("/admin/clubs/{id}/activate")
    public String activateClub(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        adminService.setClubActive(me.getId(), id, true);
        return "redirect:/admin?clubShown#clubs";
    }

    @PostMapping("/admin/leaders/{id}/send-reset")
    public String sendReset(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        boolean sent = adminService.sendResetLink(me.getId(), id);
        return "redirect:/admin?" + (sent ? "resetSent" : "resetFailed") + "#leaders";
    }

    @PostMapping("/admin/leaders/{id}/disable")
    public String disable(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        adminService.setLeaderEnabled(me.getId(), id, false);
        return "redirect:/admin?leaderDisabled#leaders";
    }

    @PostMapping("/admin/leaders/{id}/enable")
    public String enable(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        adminService.setLeaderEnabled(me.getId(), id, true);
        return "redirect:/admin?leaderEnabled#leaders";
    }
}
