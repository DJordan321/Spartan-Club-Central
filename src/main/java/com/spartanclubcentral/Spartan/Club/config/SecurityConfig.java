package com.spartanclubcentral.Spartan.Club.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Who may open which page (SPR-01, SPR-07). Replaces the Phase 1 "everything
 * is public" setup.
 *
 *   Students (no account)  every public GET page, the join request form, registration,
 *                          forgot/reset password
 *   ROLE_LEADER            /dashboard/**
 *   ROLE_ADMIN             /admin/**
 *   Anything else          blocked: every new POST/PUT/DELETE route must be listed here
 *
 * Trade-off: all other GET addresses are public, so a mistyped address shows
 * the friendly "Page not found" instead of a login page. That is safe because
 * no GET route changes data; every change is a POST with a CSRF token.
 *
 * CSRF protection is ON (Spring's default). Thymeleaf's th:action adds the
 * hidden token to every form automatically, so never disable it.
 *
 * Security headers (Phase 7): a strict Content-Security-Policy lets the
 * browser run only this site's own files, so even if some text slipped past
 * escaping it could not run as a script. It works because no page uses inline
 * scripts or styles. Pages also can't be framed by other sites (clickjacking),
 * and other sites only ever see our origin as the referrer.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Only this site's own scripts, styles, images and forms; nothing inline, no framing. */
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; form-action 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'; object-src 'none'";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, RoleBasedSuccessHandler successHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Let Spring Boot's error pages render for everyone.
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                // Logged-in areas (SPR-01). Logged-out visitors are sent to /login.
                .requestMatchers("/dashboard", "/dashboard/**").hasRole("LEADER")
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                // Public forms (POST) that need no account.
                .requestMatchers(HttpMethod.POST, "/clubs/*/requests", "/register", "/forgot-password", "/reset-password").permitAll()
                // Every other page view is public: homepage, directory, profiles, login, CSS.
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .anyRequest().denyAll())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)          // leaders -> /dashboard, admins -> /admin
                .failureUrl("/login?error")              // one generic message (SPR-06)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")                    // POST only, with CSRF token
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .sessionManagement(session -> session
                // New session ID at login, so a stolen pre-login ID is useless (SPR-07).
                .sessionFixation(fixation -> fixation.changeSessionId()));
        return http.build();
    }

    /**
     * BCrypt with cost 12: salted, and about a quarter-second per check, which
     * makes guessing passwords slow for attackers (NFR-01, SPR-02).
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Publishes login success/failure events so SecurityEventListener can audit them (SPR-08). */
    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }
}
