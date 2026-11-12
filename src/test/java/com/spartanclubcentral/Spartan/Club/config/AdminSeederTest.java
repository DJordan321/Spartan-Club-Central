package com.spartanclubcentral.Spartan.Club.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spartanclubcentral.Spartan.Club.model.Administrator;
import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.AuthService;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Unit tests for creating the administrator from environment variables. */
class AdminSeederTest {

    private final AdminRepository admins = mock(AdminRepository.class);
    private final AuthService authService = mock(AuthService.class);
    private final PasswordHasher hasher = new PasswordHasher(new BCryptPasswordEncoder(4));
    private final AuditLogger audit = mock(AuditLogger.class);

    private AdminSeeder seeder(String email, String password) {
        when(admins.save(any(Administrator.class))).thenAnswer(inv -> inv.getArgument(0));
        return new AdminSeeder(admins, authService, hasher, audit, email, password);
    }

    @Test
    void createsAdminWithHashedPasswordWhenNoneExists() {
        assertThat(seeder(" Admin@ClubCentral.local ", "demo-admin-pass").seed()).isTrue();
        ArgumentCaptor<Administrator> saved = ArgumentCaptor.forClass(Administrator.class);
        verify(admins).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@clubcentral.local");
        assertThat(saved.getValue().getPasswordHash()).startsWith("$2a$").isNotEqualTo("demo-admin-pass");
        assertThat(hasher.matches("demo-admin-pass", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void skipsWhenAnAdminAlreadyExists() {
        when(admins.count()).thenReturn(1L);
        assertThat(seeder("admin@clubcentral.local", "demo-admin-pass").seed()).isFalse();
        verify(admins, never()).save(any());
    }

    @Test
    void skipsWhenNotConfiguredOrInvalid() {
        assertThat(seeder("", "").seed()).isFalse();
        assertThat(seeder("not-an-email", "demo-admin-pass").seed()).isFalse();
        assertThat(seeder("admin@clubcentral.local", "short").seed()).isFalse();
        verify(admins, never()).save(any());
    }

    @Test
    void skipsWhenEmailBelongsToALeader() {
        when(authService.isEmailTaken("admin@clubcentral.local")).thenReturn(true);
        assertThat(seeder("admin@clubcentral.local", "demo-admin-pass").seed()).isFalse();
        verify(admins, never()).save(any());
    }
}
