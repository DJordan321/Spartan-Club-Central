package com.spartanclubcentral.Spartan.Club.service.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown by services when submitted form data breaks a rule (SPR-05).
 * Carries one friendly message per field (field name -> message) so the page
 * can show each message next to the right input.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("Invalid input: " + fieldErrors.keySet());   // field names only, never the typed values
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
