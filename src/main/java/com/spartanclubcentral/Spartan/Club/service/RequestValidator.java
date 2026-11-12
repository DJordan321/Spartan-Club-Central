package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Service layer: checks a join request form on the server (FR-03, SPR-05).
 *
 * Rules: name, email and message are required; email must look like an
 * email; nothing may be longer than FieldLimits allows (the same lengths as
 * the database columns). Browsers also limit lengths, but those limits can be
 * bypassed, so the server always checks again.
 */
@Component
public class RequestValidator {

    /** Returns field name -> message for every problem; empty when the form is valid. */
    public Map<String, String> validate(JoinRequestForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = trim(form.getName());
        String email = trim(form.getEmail());
        String message = trim(form.getMessage());

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

        if (message.isEmpty()) {
            errors.put("message", "Enter a message for the club.");
        } else if (message.length() > FieldLimits.MESSAGE) {
            errors.put("message", "Message must be " + String.format("%,d", FieldLimits.MESSAGE)
                    + " characters or fewer (yours is " + String.format("%,d", message.length()) + ").");
        }
        return errors;
    }

    static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
