package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.dto.ClubForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for the create/edit club form rules (FR-08, FR-09, SPR-05). */
class ClubValidatorTest {

    private final ClubValidator validator = new ClubValidator();

    @Test
    void validFormWithOnlyRequiredFields() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void requiredFieldsAreReported() {
        assertThat(validator.validate(new ClubForm()))
                .containsOnlyKeys("clubName", "description", "categoryId", "contactEmail");
    }

    @Test
    void lengthsMatchTheDatabase() {
        ClubForm f = valid();
        f.setClubName("n".repeat(FieldLimits.CLUB_NAME));
        f.setDescription("d".repeat(FieldLimits.DESCRIPTION));
        assertThat(validator.validate(f)).isEmpty();
        f.setClubName("n".repeat(FieldLimits.CLUB_NAME + 1));
        f.setDescription("d".repeat(FieldLimits.DESCRIPTION + 1));
        f.setMeetingTime("m".repeat(FieldLimits.MEETING_TIME + 1));
        assertThat(validator.validate(f)).containsOnlyKeys("clubName", "description", "meetingTime");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://instagram.com/chess", "http://example.com", "https://example.com/a?b=c"})
    void acceptsWebLinks(String url) {
        ClubForm f = valid();
        f.setInstagramUrl(url);
        assertThat(validator.validate(f)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "instagram.com/chess", "ftp://example.com", "https://", "not a url"})
    void rejectsNonWebLinksIncludingJavascript(String url) {
        ClubForm f = valid();
        f.setWebsiteUrl(url);
        assertThat(validator.validate(f)).containsOnlyKeys("websiteUrl");
    }

    @Test
    void badContactEmail() {
        ClubForm f = valid();
        f.setContactEmail("club@");
        assertThat(validator.validate(f)).containsOnlyKeys("contactEmail");
    }

    private static ClubForm valid() {
        ClubForm f = new ClubForm();
        f.setClubName("Robotics Club");
        f.setDescription("We build robots.");
        f.setCategoryId(1);
        f.setContactEmail("robotics@example.com");
        return f;
    }
}
