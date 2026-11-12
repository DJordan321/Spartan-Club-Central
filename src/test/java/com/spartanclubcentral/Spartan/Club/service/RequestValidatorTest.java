package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for join request validation (FR-03, SPR-05, TC-21). */
class RequestValidatorTest {

    private final RequestValidator validator = new RequestValidator();

    @Test
    void validFormHasNoErrors() {
        assertThat(validator.validate(new JoinRequestForm("Jordan Lee", "jordan@example.com", "Hi!"))).isEmpty();
    }

    @Test
    void blankOrMissingFieldsAreAllReported() {
        Map<String, String> errors = validator.validate(new JoinRequestForm("  ", null, "\n"));
        assertThat(errors).containsOnlyKeys("name", "email", "message");
        assertThat(errors.get("name")).isEqualTo("Enter your name.");
    }

    @Test
    void badEmailFormatIsRejected() {
        assertThat(validator.validate(new JoinRequestForm("Jordan", "student@", "Hi")))
                .containsOnlyKeys("email");
    }

    @Test
    void valuesExactlyAtTheLimitAreAccepted() {
        JoinRequestForm form = new JoinRequestForm("n".repeat(FieldLimits.PERSON_NAME), "a@example.com",
                "m".repeat(FieldLimits.MESSAGE));
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void valuesOverTheLimitAreRejected() {
        JoinRequestForm form = new JoinRequestForm("n".repeat(FieldLimits.PERSON_NAME + 1), "a@example.com",
                "m".repeat(FieldLimits.MESSAGE + 1));
        Map<String, String> errors = validator.validate(form);
        assertThat(errors).containsOnlyKeys("name", "message");
        assertThat(errors.get("message")).contains("1,000").contains("1,001");
    }

    @Test
    void surroundingSpacesDoNotCountTowardTheLimit() {
        JoinRequestForm form = new JoinRequestForm("  Jordan  ", " a@example.com ",
                "  " + "m".repeat(FieldLimits.MESSAGE) + "  ");
        assertThat(validator.validate(form)).isEmpty();
    }
}
