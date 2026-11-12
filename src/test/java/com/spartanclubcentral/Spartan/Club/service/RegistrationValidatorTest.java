package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.dto.RegistrationForm;
import org.junit.jupiter.api.Test;

/** Unit tests for leader registration rules (FR-04, SPR-05). */
class RegistrationValidatorTest {

    private final RegistrationValidator validator = new RegistrationValidator();

    @Test
    void validForm() {
        assertThat(validator.validate(form("Jordan", "jordan@example.com", "", "12345678", "12345678"))).isEmpty();
    }

    @Test
    void requiredFields() {
        assertThat(validator.validate(form("", "", "", "", ""))).containsOnlyKeys("name", "email", "password");
    }

    @Test
    void passwordLengthLimits() {
        assertThat(validator.validate(form("J", "j@example.com", "", "1234567", "1234567"))).containsOnlyKeys("password");
        String max = "p".repeat(72);
        assertThat(validator.validate(form("J", "j@example.com", "", max, max))).isEmpty();
        String tooLong = "p".repeat(73);
        assertThat(validator.validate(form("J", "j@example.com", "", tooLong, tooLong))).containsOnlyKeys("password");
    }

    @Test
    void passwordOverBcryptByteLimitIsRejectedEvenIfFewCharacters() {
        String emoji = "😀".repeat(20);   // 20 emoji: 40 Java chars but 80 UTF-8 bytes
        assertThat(validator.validate(form("J", "j@example.com", "", emoji, emoji))).containsOnlyKeys("password");
    }

    @Test
    void confirmMustMatch() {
        assertThat(validator.validate(form("J", "j@example.com", "", "12345678", "12345679")))
                .containsOnlyKeys("confirmPassword");
    }

    @Test
    void positionIsOptionalButLimited() {
        assertThat(validator.validate(form("J", "j@example.com", "x".repeat(51), "12345678", "12345678")))
                .containsOnlyKeys("position");
    }

    private static RegistrationForm form(String name, String email, String position, String pw, String confirm) {
        return new RegistrationForm(name, email, position, pw, confirm);
    }
}
