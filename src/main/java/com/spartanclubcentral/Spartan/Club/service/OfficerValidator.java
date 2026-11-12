package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.OfficerForm;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Service layer: checks the officer form on the server (FR-13, SPR-05). */
@Component
public class OfficerValidator {

    public Map<String, String> validate(OfficerForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = RequestValidator.trim(form.getName());
        String role = RequestValidator.trim(form.getRole());
        if (name.isEmpty()) {
            errors.put("name", "Enter the officer's name.");
        } else if (name.length() > FieldLimits.PERSON_NAME) {
            errors.put("name", "Name must be " + FieldLimits.PERSON_NAME + " characters or fewer.");
        }
        if (role.isEmpty()) {
            errors.put("role", "Enter their role, like President or Treasurer.");
        } else if (role.length() > FieldLimits.OFFICER_ROLE) {
            errors.put("role", "Role must be " + FieldLimits.OFFICER_ROLE + " characters or fewer.");
        }
        return errors;
    }
}
