package com.spartanclubcentral.Spartan.Club.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller layer: the administrator area (ROLE_ADMIN only).
 * Phase 4 proves the admin login and role check work; Phase 6 adds the
 * club and leader lists, deactivation and "send reset link".
 */
@Controller
public class AdminController {

    @GetMapping("/admin")
    public String home() {
        return "admin/home";
    }
}
