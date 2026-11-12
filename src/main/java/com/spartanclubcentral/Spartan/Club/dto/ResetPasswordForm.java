package com.spartanclubcentral.Spartan.Club.dto;

/** The new password typed on the reset page (FR-11). Never stored or re-rendered. */
public class ResetPasswordForm {

    private String token = "";
    private String password = "";
    private String confirmPassword = "";

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
