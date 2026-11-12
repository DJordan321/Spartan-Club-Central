package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.dto.EventForm;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Unit tests for event rules (FR-14, SPR-05). "Now" is fixed so the tests never depend on the clock. */
class EventValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 12, 0);
    private final EventValidator validator = new EventValidator();

    @Test
    void validEvent() {
        assertThat(validator.validate(form("Blitz", "2026-11-20", "18:00", "20:00"), true, NOW)).isEmpty();
        assertThat(validator.validate(form("Blitz", "2026-11-20", "18:00", ""), true, NOW)).isEmpty();
    }

    @Test
    void requiredFields() {
        assertThat(validator.validate(form("", "", "", ""), true, NOW)).containsOnlyKeys("title", "date", "startTime");
    }

    @Test
    void endMustBeAfterStart() {
        assertThat(validator.validate(form("Blitz", "2026-11-20", "18:00", "17:00"), true, NOW))
                .containsEntry("endTime", "The end time must be after the start time.");
        assertThat(validator.validate(form("Blitz", "2026-11-20", "18:00", "18:00"), true, NOW)).containsOnlyKeys("endTime");
    }

    @Test
    void newEventsCantStartInThePastButEditsCan() {
        assertThat(validator.validate(form("Blitz", "2026-11-11", "18:00", ""), true, NOW))
                .containsEntry("date", "New events can't start in the past.");
        assertThat(validator.validate(form("Blitz", "2026-11-11", "18:00", ""), false, NOW)).isEmpty();
    }

    @Test
    void atMostAYearAhead() {
        assertThat(validator.validate(form("Blitz", "2027-12-31", "18:00", ""), true, NOW))
                .containsEntry("date", "Events can be posted at most a year ahead.");
    }

    @Test
    void madeUpDatesAndTimesAndLengths() {
        assertThat(validator.validate(form("Blitz", "2026-02-30", "25:00", "x"), true, NOW))
                .containsOnlyKeys("date", "startTime", "endTime");
        EventForm f = form("t".repeat(101), "2026-11-20", "18:00", "");
        f.setDescription("d".repeat(1001));
        f.setLocation("l".repeat(151));
        assertThat(validator.validate(f, true, NOW)).containsOnlyKeys("title", "description", "location");
    }

    @Test
    void timesAreCombinedWithTheDate() {
        EventValidator.Times t = validator.times(form("Blitz", "2026-11-20", "18:00", "20:30"));
        assertThat(t.start()).isEqualTo(LocalDateTime.of(2026, 11, 20, 18, 0));
        assertThat(t.end()).isEqualTo(LocalDateTime.of(2026, 11, 20, 20, 30));
    }

    private static EventForm form(String title, String date, String start, String end) {
        EventForm f = new EventForm();
        f.setTitle(title);
        f.setDate(date);
        f.setStartTime(start);
        f.setEndTime(end);
        return f;
    }
}
