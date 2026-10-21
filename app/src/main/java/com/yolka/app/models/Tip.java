package com.yolka.app.models;

import java.io.Serializable;

public class Tip implements Serializable {
    private String id;
    private String uuid;
    private String category; // FEEDING | HYGIENE | EXERCISE | VACCINATION | DEWORMING | GENERAL_CARE | WELLNESS | FUN_FACTS
    private String species;  // DOG | CAT | ALL
    private String title;
    private String summary;
    private String content;
    private String imageUrl;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getCategory() { return category != null ? category : "GENERAL_CARE"; }
    public void setCategory(String category) { this.category = category; }
    public String getSpecies() { return species != null ? species : "ALL"; }
    public void setSpecies(String species) { this.species = species; }
    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary != null ? summary : ""; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content != null ? content : ""; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
