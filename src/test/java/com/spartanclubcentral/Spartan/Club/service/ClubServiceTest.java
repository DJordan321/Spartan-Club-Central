package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for ClubService with the repositories mocked (no database). */
class ClubServiceTest {

    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ClubService service = new ClubService(clubRepository, categoryRepository);

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

    @Test
    void searchPassesTheCleanedKeywordAndCategoryToTheRepository() {
        when(clubRepository.search("chess", 1)).thenReturn(List.of());
        service.search("  chess  ", 1);
        verify(clubRepository).search("chess", 1);
    }

    @Test
    void activeClubIsReturned() {
        Club club = new Club();
        when(clubRepository.findByClubIdAndActiveTrue(7L)).thenReturn(Optional.of(club));
        assertThat(service.getActiveClub(7L)).isSameAs(club);
    }

    @Test
    void missingOrInactiveClubIsNotFound() {
        when(clubRepository.findByClubIdAndActiveTrue(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getActiveClub(8L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.getActiveClub(null)).isInstanceOf(NotFoundException.class);
    }
}
