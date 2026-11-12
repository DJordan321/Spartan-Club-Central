package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.dto.EventForm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Service layer: checks the event form on the server (FR-14, SPR-05).
 *
 * Rules: title required; date and start time required and real; end time
 * optional but after the start; at most a year ahead; NEW events can't start
 * in the past (editing an event that already happened is allowed, e.g. to
 * fix a typo). Lengths come from FieldLimits.
 */
@Component
public class EventValidator {

    /** The parsed start and end, when the form is valid. */
    public record Times(LocalDateTime start, LocalDateTime end) { }

    public Map<String, String> validate(EventForm form, boolean isNew, LocalDateTime now) {
        Map<String, String> errors = new LinkedHashMap<>();
        String title = RequestValidator.trim(form.getTitle());
        if (title.isEmpty()) {
            errors.put("title", "Enter a title for the event.");
        } else if (title.length() > FieldLimits.EVENT_TITLE) {
            errors.put("title", "Title must be " + FieldLimits.EVENT_TITLE + " characters or fewer.");
        }

        LocalDate date = parseDate(form.getDate());
        LocalTime start = parseTime(form.getStartTime());
        LocalTime end = parseTime(form.getEndTime());
        if (date == null) {
            errors.put("date", RequestValidator.trim(form.getDate()).isEmpty() ? "Choose the date." : "Enter a real date.");
        }
        if (start == null) {
            errors.put("startTime", RequestValidator.trim(form.getStartTime()).isEmpty() ? "Choose the start time." : "Enter a real time.");
        }
        if (!RequestValidator.trim(form.getEndTime()).isEmpty() && end == null) {
            errors.put("endTime", "Enter a real time, or leave it empty.");
        }
        if (date != null && start != null) {
            LocalDateTime startsAt = date.atTime(start);
            if (isNew && startsAt.isBefore(now)) {
                errors.put("date", "New events can't start in the past.");
            } else if (startsAt.isAfter(now.plusDays(FieldLimits.EVENT_MAX_DAYS_AHEAD))) {
                errors.put("date", "Events can be posted at most a year ahead.");
            }
            if (end != null && !end.isAfter(start)) {
                errors.put("endTime", "The end time must be after the start time.");
            }
        }

        String description = RequestValidator.trim(form.getDescription());
        if (description.length() > FieldLimits.EVENT_DESCRIPTION) {
            errors.put("description", "Description must be " + String.format("%,d", FieldLimits.EVENT_DESCRIPTION)
                    + " characters or fewer (yours is " + String.format("%,d", description.length()) + ").");
        }
        String location = RequestValidator.trim(form.getLocation());
        if (location.length() > FieldLimits.EVENT_LOCATION) {
            errors.put("location", "Location must be " + FieldLimits.EVENT_LOCATION + " characters or fewer.");
        }
        return errors;
    }

    /** Start and end as date-times; call only after validate() returned no errors. */
    public Times times(EventForm form) {
        LocalDate date = parseDate(form.getDate());
        LocalTime end = parseTime(form.getEndTime());
        return new Times(date.atTime(parseTime(form.getStartTime())), end == null ? null : date.atTime(end));
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(RequestValidator.trim(raw));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(RequestValidator.trim(raw));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
