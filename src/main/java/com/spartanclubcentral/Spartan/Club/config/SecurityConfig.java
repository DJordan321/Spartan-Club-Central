package com.spartanclubcentral.Spartan.Club.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY security setup for Phase 1 so the app starts with no login pages yet.
 *
 * Everything is public for now. CSRF protection stays ON (the default).
 * Phase 4 replaces this file with the real rules: public student pages,
 * /dashboard/** for ROLE_LEADER, /admin/** for ROLE_ADMIN (SPR-01).
 * Do NOT demo or deploy with this version.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
