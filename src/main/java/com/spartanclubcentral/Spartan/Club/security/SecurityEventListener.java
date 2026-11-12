package com.spartanclubcentral.Spartan.Club.security;

import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Security layer: turns Spring Security's login/logout events into audit log
 * entries (SPR-08). A failed login is recorded as ANONYMOUS with no email:
 * the typed email might be someone else's, or a mistyped password.
 */
@Component
public class SecurityEventListener {

    private final AuditLogger audit;

    public SecurityEventListener(AuditLogger audit) {
        this.audit = audit;
    }

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        record(event.getAuthentication(), AuditLogger.LOGIN_SUCCESS);
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        audit.log("ANONYMOUS", null, AuditLogger.LOGIN_FAILED, null, null, false);
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        record(event.getAuthentication(), AuditLogger.LOGOUT);
    }

    private void record(Authentication authentication, String action) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal p) {
            audit.log(p.getRole().name(), p.getId(), action, null, null, true);
        }
    }
}
