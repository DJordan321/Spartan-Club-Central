package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Unit tests for the email sent to a club (FR-07), with the mail server mocked. */
class NotificationServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final NotificationService service = new NotificationService(mailSender, "no-reply@clubcentral.local");

    @Test
    void emailGoesToTheClubWithReplyToTheStudent() {
        assertThat(service.notifyClub(club(), request())).isTrue();

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        SimpleMailMessage mail = sent.getValue();
        assertThat(mail.getTo()).containsExactly("chess@example.com");
        assertThat(mail.getReplyTo()).isEqualTo("jordan@example.com");
        assertThat(mail.getFrom()).isEqualTo("no-reply@clubcentral.local");
        assertThat(mail.getSubject()).isEqualTo("New join request for Chess Club");
        assertThat(mail.getText()).contains("Name:  Jordan Lee", "Email: jordan@example.com", "I love chess!");
    }

    @Test
    void mailServerDownReturnsFalseInsteadOfThrowing() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThat(service.notifyClub(club(), request())).isFalse();
    }

    private static Club club() {
        Club c = new Club();
        c.setClubName("Chess Club");
        c.setContactEmail("chess@example.com");
        return c;
    }

    private static JoinRequest request() {
        JoinRequest r = new JoinRequest();
        r.setStudentName("Jordan Lee");
        r.setStudentEmail("jordan@example.com");
        r.setMessage("I love chess!");
        return r;
    }
}
