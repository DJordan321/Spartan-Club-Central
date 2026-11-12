package com.spartanclubcentral.Spartan.Club.dto;

import com.spartanclubcentral.Spartan.Club.model.ClubOfficer;

/** What a leader typed when adding or editing an officer (FR-13). */
public class OfficerForm {

    private String name = "";
    private String role = "";

    public OfficerForm() {
    }

    public OfficerForm(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public static OfficerForm from(ClubOfficer officer) {
        return new OfficerForm(officer.getName(), officer.getRole());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
