package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.RegistrationForm;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Service layer: checks the leader registration form on the server (FR-04, SPR-05).
 *
 * Password rule: 8 to 72 characters. BCrypt ignores everything after 72
 * bytes, so longer passwords are rejected instead of being silently cut.
 * (Accented letters and emoji take more than one byte, so the byte length is
 * checked too.) Whether the email is already taken is checked by AuthService,
 * because that needs the database.
 */
@Component
public class RegistrationValidator {

    public Map<String, String> validate(RegistrationForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = RequestValidator.trim(form.getName());
        String email = RequestValidator.trim(form.getEmail());
        String position = RequestValidator.trim(form.getPosition());
        String password = form.getPassword() == null ? "" : form.getPassword();
        String confirm = form.getConfirmPassword() == null ? "" : form.getConfirmPassword();

        if (name.isEmpty()) {
            errors.put("name", "Enter your name.");
        } else if (name.length() > FieldLimits.PERSON_NAME) {
            errors.put("name", "Name must be " + FieldLimits.PERSON_NAME + " characters or fewer.");
        }

        if (email.isEmpty()) {
            errors.put("email", "Enter your email address.");
        } else if (!EmailRules.isValid(email)) {
            errors.put("email", "Enter a valid email address, like name@example.com.");
        }

        if (position.length() > FieldLimits.POSITION) {
            errors.put("position", "Position must be " + FieldLimits.POSITION + " characters or fewer.");
        }

        if (password.isEmpty()) {
            errors.put("password", "Choose a password.");
        } else if (password.length() < FieldLimits.PASSWORD_MIN) {
            errors.put("password", "Password must be at least " + FieldLimits.PASSWORD_MIN + " characters.");
        } else if (password.getBytes(StandardCharsets.UTF_8).length > FieldLimits.PASSWORD_MAX) {
            errors.put("password", "Password must be " + FieldLimits.PASSWORD_MAX + " characters or fewer.");
        }

        if (!errors.containsKey("password") && !password.equals(confirm)) {
            errors.put("confirmPassword", "The two passwords don't match.");
        }
        return errors;
    }
}
