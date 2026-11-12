package com.spartanclubcentral.Spartan.Club.dto;

import com.spartanclubcentral.Spartan.Club.model.ClubEvent;
import java.time.format.DateTimeFormatter;

/**
 * What a leader typed into the event form (FR-14). Date and times come from
 * <input type="date"> (yyyy-MM-dd) and <input type="time"> (HH:mm), which
 * work without JavaScript. An event happens on one day.
 */
public class EventForm {

    private String title = "";
    private String description = "";
    private String date = "";
    private String startTime = "";
    private String endTime = "";
    private String location = "";

    public static EventForm from(ClubEvent e) {
        EventForm f = new EventForm();
        f.title = e.getTitle();
        f.description = e.getDescription() == null ? "" : e.getDescription();
        f.date = e.getStartsAt().toLocalDate().toString();
        f.startTime = e.getStartsAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        f.endTime = e.getEndsAt() == null ? "" : e.getEndsAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        f.location = e.getLocation() == null ? "" : e.getLocation();
        return f;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
