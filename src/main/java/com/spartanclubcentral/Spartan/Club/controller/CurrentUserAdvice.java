package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller layer: makes the logged-in account (or null) available to every
 * page as ${currentUser}, so the shared header can show "Leader login" or
 * "Dashboard / Log out". Only the name and role are used in templates.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public AccountPrincipal currentUser(@AuthenticationPrincipal AccountPrincipal principal) {
        return principal;
    }
}
