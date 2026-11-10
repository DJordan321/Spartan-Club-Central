package com.spartanclubcentral.Spartan.Club.service;

import java.util.regex.Pattern;

/**
 * Email format check shared by every form that takes an email (SPR-05).
 *
 * Deliberately simple: something@something.something, no spaces, within the
 * length limit. Full RFC validation rejects real addresses more often than it
 * catches typos, and the real proof is whether the email arrives.
 */
public final class EmailRules {

    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailRules() {
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= FieldLimits.EMAIL
                && SIMPLE_EMAIL.matcher(trimmed).matches();
    }
}
