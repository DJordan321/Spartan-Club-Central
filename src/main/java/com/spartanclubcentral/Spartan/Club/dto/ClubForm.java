package com.spartanclubcentral.Spartan.Club.dto;

import com.spartanclubcentral.Spartan.Club.model.Club;

/**
 * What a leader typed into the create/edit club form (FR-08, FR-09).
 * Checked by ClubValidator and ClubService before anything is saved.
 */
public class ClubForm {

    private String clubName = "";
    private String description = "";
    private Integer categoryId;
    private String meetingTime = "";
    private String contactEmail = "";
    private String contactInfo = "";
    private String instagramUrl = "";
    private String websiteUrl = "";
    private String otherSocialUrl = "";

    /** A form filled with a club's current values, for the edit page. */
    public static ClubForm from(Club club) {
        ClubForm f = new ClubForm();
        f.clubName = club.getClubName();
        f.description = club.getDescription();
        f.categoryId = club.getCategory().getCategoryId();
        f.meetingTime = orEmpty(club.getMeetingTime());
        f.contactEmail = club.getContactEmail();
        f.contactInfo = orEmpty(club.getContactInfo());
        f.instagramUrl = orEmpty(club.getInstagramUrl());
        f.websiteUrl = orEmpty(club.getWebsiteUrl());
        f.otherSocialUrl = orEmpty(club.getOtherSocialUrl());
        return f;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getMeetingTime() { return meetingTime; }
    public void setMeetingTime(String meetingTime) { this.meetingTime = meetingTime; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getOtherSocialUrl() { return otherSocialUrl; }
    public void setOtherSocialUrl(String otherSocialUrl) { this.otherSocialUrl = otherSocialUrl; }
}
