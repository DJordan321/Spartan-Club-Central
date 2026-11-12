package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.JoinRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service layer: sends emails (FR-07). On a demo laptop they go to Mailpit
 * (http://localhost:8025), so nothing real is ever sent.
 *
 * Never throws: if the mail server is down it returns false and the caller
 * records the request as FAILED. An email problem must not lose a saved
 * request or crash the page (the cause of TC-07).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NotificationService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Emails a new join request to the club's contact address. Reply-To is the
     * student, so the leader can simply press Reply.
     *
     * @return true if the mail server accepted the email, false otherwise
     */
    public boolean notifyClub(Club club, JoinRequest request) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(club.getContactEmail());
            mail.setReplyTo(request.getStudentEmail());
            mail.setSubject("New join request for " + club.getClubName());
            mail.setText(buildBody(club, request));
            mailSender.send(mail);
            return true;
        } catch (MailException e) {
            // Request id and reason only: no student name, email or message in logs (SPR-08).
            log.warn("Join request {} could not be emailed: {}", request.getRequestId(), e.getClass().getSimpleName());
            return false;
        }
    }

    static String buildBody(Club club, JoinRequest request) {
        return "Hello " + club.getClubName() + ",\n\n"
                + "A student sent a join/contact request through Spartan Club Central.\n\n"
                + "Name:  " + request.getStudentName() + "\n"
                + "Email: " + request.getStudentEmail() + "\n\n"
                + "Message:\n" + request.getMessage() + "\n\n"
                + "Reply to this email to answer the student directly.\n\n"
                + "Spartan Club Central\n";
    }

    /**
     * Emails a one-time password reset link (FR-11). The link is the only
     * place the raw token ever exists; the database keeps just its hash.
     *
     * @return true if the mail server accepted the email
     */
    public boolean sendResetLink(String toEmail, String leaderName, String link) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(toEmail);
            mail.setSubject("Reset your Spartan Club Central password");
            mail.setText("Hello " + leaderName + ",\n\n"
                    + "Someone asked to reset the password for your club leader account.\n"
                    + "To choose a new password, open this link within 30 minutes:\n\n"
                    + link + "\n\n"
                    + "The link works once. If you didn't ask for this, you can ignore this email;\n"
                    + "your password stays the same.\n\n"
                    + "Spartan Club Central\n");
            mailSender.send(mail);
            return true;
        } catch (MailException e) {
            log.warn("Password reset email could not be sent: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
