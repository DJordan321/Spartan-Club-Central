package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for ClubService with the repositories mocked (no database). */
class ClubServiceTest {

    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final LeaderRepository leaderRepository = mock(LeaderRepository.class);
    private final RequestRepository requestRepository = mock(RequestRepository.class);
    private final AuditLogger audit = mock(AuditLogger.class);
    private final ClubService service = new ClubService(clubRepository, categoryRepository, leaderRepository,
            requestRepository, new ClubValidator(), audit);

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

    @Test
    void ownershipCheckPassesOnlyForTheOwningLeader() {
        Club club = new Club();
        com.spartanclubcentral.Spartan.Club.model.ClubLeader owner = new com.spartanclubcentral.Spartan.Club.model.ClubLeader();
        org.springframework.test.util.ReflectionTestUtils.setField(club, "clubId", 5L);
        owner.setClub(club);
        when(clubRepository.findById(5L)).thenReturn(Optional.of(club));
        when(leaderRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(leaderRepository.findById(2L)).thenReturn(Optional.of(new com.spartanclubcentral.Spartan.Club.model.ClubLeader()));

        assertThat(service.requireOwnedClub(1L, 5L)).isSameAs(club);
        assertThatThrownBy(() -> service.requireOwnedClub(2L, 5L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(audit).log("LEADER", 2L, AuditLogger.OWNERSHIP_DENIED, "CLUB", 5L, false);
    }
}
