package com.spartanclubcentral.Spartan.Club.config;

import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

/**
 * Where to go after a successful login (FR-04).
 *
 * If the user was sent to the login page from a protected page in THEIR OWN
 * area (a leader from /dashboard/..., an admin from /admin/...), they go back
 * to it. Otherwise leaders go to /dashboard and administrators to /admin.
 * Without the "own area" check, an admin who logs in after someone tried
 * /dashboard on the same browser would land on an "access denied" page.
 */
@Component
public class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    public RoleBasedSuccessHandler() {
        setRequestCache(requestCache);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String home = homeFor(authentication);
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null && isInArea(saved.getRedirectUrl(), home)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        requestCache.removeRequest(request, response);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, home);
    }

    static String homeFor(Authentication authentication) {
        boolean admin = authentication.getPrincipal() instanceof AccountPrincipal p && p.isAdmin();
        return admin ? "/admin" : "/dashboard";
    }

    /** True if the saved address is the home page or a page under it, e.g. /dashboard/clubs/3/edit. */
    static boolean isInArea(String savedUrl, String home) {
        String path = URI.create(savedUrl).getPath();
        return path != null && (path.equals(home) || path.startsWith(home + "/"));
    }
}
