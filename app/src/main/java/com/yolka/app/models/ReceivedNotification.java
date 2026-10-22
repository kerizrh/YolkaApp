package com.yolka.app.models;

import java.io.Serializable;
import java.util.UUID;

public class ReceivedNotification implements Serializable {

    private String id;
    private String title;
    private String body;
    private String category;
    private String petName;
    private long timestamp;
    private boolean read;

    public ReceivedNotification() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public ReceivedNotification(String title, String body, String category, String petName) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.body = body;
        this.category = category != null ? category : "GENERAL";
        this.petName = petName != null ? petName : "";
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body != null ? body : ""; }
    public void setBody(String body) { this.body = body; }

    public String getCategory() { return category != null ? category : "GENERAL"; }
    public void setCategory(String category) { this.category = category; }

    public String getPetName() { return petName != null ? petName : ""; }
    public void setPetName(String petName) { this.petName = petName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
