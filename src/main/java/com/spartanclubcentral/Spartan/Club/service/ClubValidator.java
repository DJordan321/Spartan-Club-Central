package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.ClubForm;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Service layer: checks the create/edit club form on the server (FR-08, FR-09, SPR-05).
 *
 * Lengths come from FieldLimits (the same as the database columns). Links
 * must be full http:// or https:// web addresses, so nobody can save a
 * "javascript:" link that would run code when a student clicks it.
 * Whether the name is already used and whether the category exists are
 * checked in ClubService, because that needs the database.
 */
@Component
public class ClubValidator {

    public Map<String, String> validate(ClubForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        required(errors, "clubName", form.getClubName(), FieldLimits.CLUB_NAME, "Enter the club's name.", "Club name");
        required(errors, "description", form.getDescription(), FieldLimits.DESCRIPTION,
                "Describe what the club does.", "Description");
        if (form.getCategoryId() == null) {
            errors.put("categoryId", "Choose a category.");
        }
        optional(errors, "meetingTime", form.getMeetingTime(), FieldLimits.MEETING_TIME, "Meeting time");

        String email = RequestValidator.trim(form.getContactEmail());
        if (email.isEmpty()) {
            errors.put("contactEmail", "Enter the email address join requests should go to.");
        } else if (!EmailRules.isValid(email)) {
            errors.put("contactEmail", "Enter a valid email address, like club@example.com.");
        }

        optional(errors, "contactInfo", form.getContactInfo(), FieldLimits.CONTACT_INFO, "Contact info");
        url(errors, "instagramUrl", form.getInstagramUrl());
        url(errors, "websiteUrl", form.getWebsiteUrl());
        url(errors, "otherSocialUrl", form.getOtherSocialUrl());
        return errors;
    }

    /** True for a full web address starting with http:// or https:// that has a host name. */
    static boolean isWebUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private static void required(Map<String, String> errors, String field, String raw, int max,
                                 String missingMessage, String label) {
        String value = RequestValidator.trim(raw);
        if (value.isEmpty()) {
            errors.put(field, missingMessage);
        } else if (value.length() > max) {
            errors.put(field, tooLong(label, max, value.length()));
        }
    }

    private static void optional(Map<String, String> errors, String field, String raw, int max, String label) {
        String value = RequestValidator.trim(raw);
        if (value.length() > max) {
            errors.put(field, tooLong(label, max, value.length()));
        }
    }

    private static void url(Map<String, String> errors, String field, String raw) {
        String value = RequestValidator.trim(raw);
        if (value.isEmpty()) {
            return;
        }
        if (value.length() > FieldLimits.URL) {
            errors.put(field, tooLong("Link", FieldLimits.URL, value.length()));
        } else if (!isWebUrl(value)) {
            errors.put(field, "Enter a full web address starting with https://");
        }
    }

    private static String tooLong(String label, int max, int actual) {
        return label + " must be " + String.format("%,d", max) + " characters or fewer (yours is "
                + String.format("%,d", actual) + ").";
    }
}
