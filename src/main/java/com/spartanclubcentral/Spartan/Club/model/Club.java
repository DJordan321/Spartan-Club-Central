package com.spartanclubcentral.Spartan.Club.model;

import com.spartanclubcentral.Spartan.Club.service.FieldLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Domain model: one club listing (FR-05, FR-06, FR-10).
 *
 * Holds data only. Search, ownership and validation rules live in the
 * service layer. Every column is mapped by name so a mismatch with
 * schema.sql stops the app at startup instead of showing blank fields (TC-06).
 */
@Entity
@Table(name = "club")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "club_name", nullable = false, unique = true, length = FieldLimits.CLUB_NAME)
    private String clubName;

    @Column(name = "description", nullable = false, length = FieldLimits.DESCRIPTION)
    private String description;

    // Eager (the JPA default for ManyToOne): every page that shows a club also shows its category.
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "meeting_time", length = FieldLimits.MEETING_TIME)
    private String meetingTime;

    @Column(name = "contact_email", nullable = false, length = FieldLimits.EMAIL)
    private String contactEmail;

    @Column(name = "contact_info", length = FieldLimits.CONTACT_INFO)
    private String contactInfo;

    @Column(name = "instagram_url", length = FieldLimits.URL)
    private String instagramUrl;

    @Column(name = "website_url", length = FieldLimits.URL)
    private String websiteUrl;

    @Column(name = "other_social_url", length = FieldLimits.URL)
    private String otherSocialUrl;

    /** Uploaded picture's file name in UPLOAD_DIR (made by the server, never the user's file name). */
    @Column(name = "image_filename", length = FieldLimits.IMAGE_FILENAME)
    private String imageFilename;

    @Column(name = "image_alt", length = FieldLimits.IMAGE_ALT)
    private String imageAlt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    void touch() {
        lastUpdated = LocalDateTime.now();
    }

    public Long getClubId() { return clubId; }
    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
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
    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }
    public String getImageAlt() { return imageAlt; }
    public void setImageAlt(String imageAlt) { this.imageAlt = imageAlt; }
    public boolean hasImage() { return imageFilename != null; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
}
