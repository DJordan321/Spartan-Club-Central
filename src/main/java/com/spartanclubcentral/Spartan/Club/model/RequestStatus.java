package com.spartanclubcentral.Spartan.Club.model;

/**
 * Lifecycle of a join request (FR-03, FR-07).
 * DELIVERED means the mail server accepted the email, not that the leader read it.
 */
public enum RequestStatus {
    SUBMITTED,
    DELIVERED,
    FAILED
}
