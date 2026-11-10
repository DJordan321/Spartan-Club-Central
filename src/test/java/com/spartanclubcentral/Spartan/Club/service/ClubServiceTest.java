package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for keyword clean-up in ClubService. */
class ClubServiceTest {

    @Test
    void blankKeywordMeansNoKeyword() {
        assertThat(ClubService.normalizeKeyword(null)).isNull();
        assertThat(ClubService.normalizeKeyword("   ")).isNull();
    }

    @Test
    void keywordIsTrimmed() {
        assertThat(ClubService.normalizeKeyword("  chess ")).isEqualTo("chess");
    }

    @Test
    void longKeywordIsCutToTheLimit() {
        assertThat(ClubService.normalizeKeyword("x".repeat(500))).hasSize(FieldLimits.SEARCH);
    }
}
