package com.spartanclubcentral.Spartan.Club.model;

import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Domain model: a student's join/contact request (FR-03).
 *
 * Contains student contact details, so it must only ever reach the leader of
 * the club it was sent to (NFR-02, SPR-04). Not tied to a login: students
 * never have accounts.
 */
@Entity
@Table(name = "join_request")
public class JoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "student_name", nullable = false, length = FieldLimits.PERSON_NAME)
    private String studentName;

    @Column(name = "student_email", nullable = false, length = FieldLimits.EMAIL)
    private String studentEmail;

    @Column(name = "message", nullable = false, length = FieldLimits.MESSAGE)
    private String message;

    // Stored as plain text (VARCHAR + CHECK in schema.sql), not a MySQL ENUM column.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private RequestStatus status = RequestStatus.SUBMITTED;

    @Column(name = "date_submitted", nullable = false, updatable = false)
    private LocalDateTime dateSubmitted;

    @PrePersist
    void onCreate() {
        if (dateSubmitted == null) {
            dateSubmitted = LocalDateTime.now();
        }
        if (status == null) {
            status = RequestStatus.SUBMITTED;
        }
    }

    public Long getRequestId() { return requestId; }
    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public LocalDateTime getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(LocalDateTime dateSubmitted) { this.dateSubmitted = dateSubmitted; }
}
