package com.yolka.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VetClinic implements Serializable {
    private String id;
    private String uuid;
    private String name;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private String openingHours;
    private List<String> services = new ArrayList<>();
    private transient double distanceMeters = -1;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address != null ? address : ""; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone != null ? phone : ""; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getOpeningHours() { return openingHours != null ? openingHours : ""; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public List<String> getServices() { return services != null ? services : new ArrayList<>(); }
    public void setServices(List<String> services) { this.services = services; }
    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
}
