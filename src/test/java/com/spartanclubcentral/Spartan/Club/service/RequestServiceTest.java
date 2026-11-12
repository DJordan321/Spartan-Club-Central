package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.spartanclubcentral.Spartan.Club.dto.JoinRequestForm;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import com.spartanclubcentral.Spartan.Club.model.RequestStatus;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RequestService with everything around it mocked:
 * no database and no mail server needed.
 */
class RequestServiceTest {

    private final ClubService clubService = mock(ClubService.class);
    private final RequestRepository repository = mock(RequestRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final RequestService service =
            new RequestService(new RequestValidator(), clubService, repository, notifications, mock(AuditLogger.class));

    private final Club club = new Club();
    /** Status of the request each time it was saved, in order. */
    private final List<RequestStatus> savedStatuses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        club.setClubName("Chess Club");
        club.setContactEmail("chess@example.com");
        when(clubService.getActiveClub(1L)).thenReturn(club);
        when(repository.save(any(JoinRequest.class))).thenAnswer(inv -> {
            JoinRequest r = inv.getArgument(0);
            savedStatuses.add(r.getStatus());
            return r;
        });
    }

    @Test
    void validRequestIsSavedThenEmailedThenMarkedDelivered() {
        when(notifications.notifyClub(any(), any())).thenReturn(true);

        JoinRequest result = service.submit(1L, new JoinRequestForm(" Jordan ", " jordan@example.com ", " Hi! "));

        assertThat(savedStatuses).containsExactly(RequestStatus.SUBMITTED, RequestStatus.DELIVERED);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.DELIVERED);
        assertThat(result.getStudentName()).isEqualTo("Jordan");
        assertThat(result.getStudentEmail()).isEqualTo("jordan@example.com");
        assertThat(result.getMessage()).isEqualTo("Hi!");
        assertThat(result.getClub()).isSameAs(club);
        assertThat(result.getDateSubmitted()).isNotNull();
    }

    @Test
    void tc10EmailFailureStillKeepsTheRequestAsFailed() {
        when(notifications.notifyClub(any(), any())).thenReturn(false);

        JoinRequest result = service.submit(1L, new JoinRequestForm("Jordan", "jordan@example.com", "Hi"));

        assertThat(savedStatuses).containsExactly(RequestStatus.SUBMITTED, RequestStatus.FAILED);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.FAILED);
    }

    @Test
    void invalidRequestSavesNothingAndSendsNothing() {
        assertThatThrownBy(() -> service.submit(1L, new JoinRequestForm("", "student@", "")))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getFieldErrors())
                        .containsOnlyKeys("name", "email", "message"));
        verifyNoInteractions(repository, notifications);
    }

    @Test
    void inactiveOrUnknownClubSavesNothing() {
        when(clubService.getActiveClub(99L)).thenThrow(new NotFoundException("Club not found"));
        assertThatThrownBy(() -> service.submit(99L, new JoinRequestForm("Jordan", "jordan@example.com", "Hi")))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(notifications);
    }
}
