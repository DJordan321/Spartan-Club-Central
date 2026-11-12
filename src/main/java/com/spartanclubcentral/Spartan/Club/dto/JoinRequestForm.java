package com.spartanclubcentral.Spartan.Club.dto;

/**
 * What the student typed into the join/contact form on a club page (FR-03).
 *
 * A plain holder for form data. It is checked by RequestValidator before
 * anything is saved, and is never stored directly.
 */
public class JoinRequestForm {

    private String name = "";
    private String email = "";
    private String message = "";

    public JoinRequestForm() {
    }

    public JoinRequestForm(String name, String email, String message) {
        this.name = name;
        this.email = email;
        this.message = message;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
