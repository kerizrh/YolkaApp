package com.yolka.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationItem implements Serializable {
    private String id;
    private String uuid;
    private String userId;
    private String petId;
    private String petName;
    private String category; // VACCINE | DEWORMING | APPOINTMENT | GROOMING | FEEDING | WALK
    private String title;
    private String body;
    private String scheduledTime;
    private String nextNotificationDate;
    private String timeZone;
    private String frequencyUnit;
    private int frequencyInterval;
    private List<String> daysOfWeek = new ArrayList<>();
    private boolean active = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public String getPetName() { return petName != null ? petName : ""; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getCategory() { return category != null ? category : "GENERAL"; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body != null ? body : ""; }
    public void setBody(String body) { this.body = body; }
    public String getScheduledTime() { return scheduledTime != null ? scheduledTime : ""; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    public String getNextNotificationDate() { return nextNotificationDate; }
    public void setNextNotificationDate(String nextNotificationDate) { this.nextNotificationDate = nextNotificationDate; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public String getFrequencyUnit() { return frequencyUnit; }
    public void setFrequencyUnit(String frequencyUnit) { this.frequencyUnit = frequencyUnit; }
    public int getFrequencyInterval() { return frequencyInterval; }
    public void setFrequencyInterval(int frequencyInterval) { this.frequencyInterval = frequencyInterval; }
    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
