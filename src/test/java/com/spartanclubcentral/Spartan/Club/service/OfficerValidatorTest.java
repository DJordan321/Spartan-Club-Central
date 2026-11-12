package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.dto.OfficerForm;
import org.junit.jupiter.api.Test;

/** Unit tests for officer rules (FR-13). */
class OfficerValidatorTest {

    private final OfficerValidator validator = new OfficerValidator();

    @Test
    void validOfficer() {
        assertThat(validator.validate(new OfficerForm("Priya Patel", "Treasurer"))).isEmpty();
    }

    @Test
    void bothFieldsRequired() {
        assertThat(validator.validate(new OfficerForm(" ", null))).containsOnlyKeys("name", "role");
    }

    @Test
    void lengthsMatchTheDatabase() {
        assertThat(validator.validate(new OfficerForm("n".repeat(100), "r".repeat(50)))).isEmpty();
        assertThat(validator.validate(new OfficerForm("n".repeat(101), "r".repeat(51)))).containsOnlyKeys("name", "role");
    }
}
