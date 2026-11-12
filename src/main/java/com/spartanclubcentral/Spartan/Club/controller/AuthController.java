package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.dto.RegistrationForm;
import com.spartanclubcentral.Spartan.Club.dto.ResetPasswordForm;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controller layer: login and registration pages (FR-04).
 *
 *   GET  /login      login form (POST /login is handled by Spring Security)
 *   GET  /register   leader registration form
 *   POST /register   create the account, then go to the login page
 *   GET/POST /forgot-password   ask for a reset link (same answer for every email)
 *   GET/POST /reset-password    choose a new password from the emailed link (FR-11)
 *
 * Someone already logged in is sent to their home page instead of login/register.
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

    // ---------- Forgot / reset password (FR-11) ----------

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam(name = "email", required = false) String email) {
        authService.requestReset(email);
        return "redirect:/forgot-password?sent";      // same result whether or not the account exists
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(name = "token", required = false) String token, Model model) {
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("valid", authService.isResetTokenValid(token));
        model.addAttribute("errors", Map.of());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@ModelAttribute ResetPasswordForm form, Model model) {
        try {
            authService.resetPassword(form);
            return "redirect:/login?reset";
        } catch (ValidationException e) {
            model.addAttribute("token", form.getToken());
            model.addAttribute("valid", !e.getFieldErrors().containsKey("token"));
            model.addAttribute("errors", e.getFieldErrors());
            return "reset-password";
        }
    }

    private static String home(AccountPrincipal me) {
        return me.isAdmin() ? "/admin" : "/dashboard";
    }
}
