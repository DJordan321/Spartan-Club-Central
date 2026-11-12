package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller layer: the club leader's dashboard (ROLE_LEADER only, SPR-01).
 *
 * Phase 4 shows a welcome page proving login works. Phase 5 adds creating,
 * editing and deactivating the leader's club and the request inbox, with the
 * ownership check (SPR-03, SPR-04). The leader is always identified from the
 * login session ($me), never from the URL or a form field.
 */
@Controller
public class DashboardController {

    private final AuthService authService;

    public DashboardController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public String home(@AuthenticationPrincipal AccountPrincipal me, Model model) {
        model.addAttribute("leader", authService.getLeader(me.getId()));
        return "dashboard/home";
    }
}
