package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for the shared email format rule (SPR-05). */
class EmailRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"student@example.com", "first.last@mail.example.edu", "  padded@example.com  "})
    void acceptsNormalAddresses(String email) {
        assertThat(EmailRules.isValid(email)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"student@", "student", "@example.com", "two@@example.com",
            "has space@example.com", "student@example"})
    void rejectsBadAddresses(String email) {
        assertThat(EmailRules.isValid(email)).isFalse();
    }

    @Test
    void rejectsAddressesLongerThanTheLimit() {
        String local = "a".repeat(FieldLimits.EMAIL);
        assertThat(EmailRules.isValid(local + "@example.com")).isFalse();
    }
}
