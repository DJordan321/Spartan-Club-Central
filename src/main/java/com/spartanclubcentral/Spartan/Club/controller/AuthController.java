package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.RegistrationForm;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controller layer: login and registration pages (FR-04).
 *
 *   GET  /login      login form (POST /login is handled by Spring Security)
 *   GET  /register   leader registration form
 *   POST /register   create the account, then go to the login page
 *
 * Someone already logged in is sent to their home page instead.
 * Phase 6 adds forgot/reset password here.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AccountPrincipal me) {
        return me != null ? "redirect:" + home(me) : "login";
    }

    @GetMapping("/register")
    public String registerForm(@AuthenticationPrincipal AccountPrincipal me, Model model) {
        if (me != null) {
            return "redirect:" + home(me);
        }
        model.addAttribute("form", new RegistrationForm());
        model.addAttribute("errors", Map.of());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegistrationForm form, Model model) {
        try {
            authService.register(form);
            return "redirect:/login?registered";
        } catch (ValidationException e) {
            form.clearPasswords();   // never send a password back to the browser (SPR-02)
            model.addAttribute("errors", e.getFieldErrors());
            return "register";
        }
    }

    private static String home(AccountPrincipal me) {
        return me.isAdmin() ? "/admin" : "/dashboard";
    }
}
