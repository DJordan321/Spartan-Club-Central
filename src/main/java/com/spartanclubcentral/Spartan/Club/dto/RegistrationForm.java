package com.spartanclubcentral.Spartan.Club.dto;

/**
 * What a club leader typed into the registration form (FR-04).
 * Passwords are only held long enough to be checked and hashed; they are
 * never stored, logged, or sent back to the page (SPR-02).
 */
public class RegistrationForm {

    private String name = "";
    private String email = "";
    private String position = "";
    private String password = "";
    private String confirmPassword = "";

    public RegistrationForm() {
    }

    public RegistrationForm(String name, String email, String position, String password, String confirmPassword) {
        this.name = name;
        this.email = email;
        this.position = position;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    /** Clears both password fields so they are never rendered back into the page. */
    public void clearPasswords() {
        password = "";
        confirmPassword = "";
    }
}
