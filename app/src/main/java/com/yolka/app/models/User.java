package com.yolka.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String id;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String photoUrl;
    private String authProvider;
    private boolean google;
    private boolean confirmed;
    private UserPreferences preferences = new UserPreferences();
    private List<DeviceInfo> activeDevices = new ArrayList<>();
    private boolean hasDeviceBackup;
    private String lastDeviceBackupAt;
    private String createdAt;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getFirstName() {
        if (firstName != null && !firstName.isBlank()) return firstName;
        if (fullName != null && !fullName.isBlank()) return fullName.split("\\s+")[0];
        return "Usuario";
    }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public boolean isGoogle() { return google; }
    public void setGoogle(boolean google) { this.google = google; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public UserPreferences getPreferences() {
        if (preferences == null) preferences = new UserPreferences();
        return preferences;
    }
    public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }

    public List<DeviceInfo> getActiveDevices() { return activeDevices; }
    public void setActiveDevices(List<DeviceInfo> activeDevices) { this.activeDevices = activeDevices; }

    public boolean isHasDeviceBackup() { return hasDeviceBackup; }
    public void setHasDeviceBackup(boolean hasDeviceBackup) { this.hasDeviceBackup = hasDeviceBackup; }

    public String getLastDeviceBackupAt() { return lastDeviceBackupAt; }
    public void setLastDeviceBackupAt(String lastDeviceBackupAt) { this.lastDeviceBackupAt = lastDeviceBackupAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public static class DeviceInfo implements Serializable {
        private String deviceId;
        private String fcmToken;

        public DeviceInfo() {}
        public DeviceInfo(String deviceId, String fcmToken) {
            this.deviceId = deviceId;
            this.fcmToken = fcmToken;
        }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    }

    public static class UserPreferences implements Serializable {
        private boolean darkMode = false;
        private String language = "es";
        private String timeZone = "America/Mexico_City";
        private NotificationPreferences notifications = new NotificationPreferences();

        public boolean isDarkMode() { return darkMode; }
        public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
        public String getLanguage() { return language != null ? language : "es"; }
        public void setLanguage(String language) { this.language = language; }
        public String getTimeZone() { return timeZone != null ? timeZone : "America/Mexico_City"; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
        public NotificationPreferences getNotifications() {
            if (notifications == null) notifications = new NotificationPreferences();
            return notifications;
        }
        public void setNotifications(NotificationPreferences notifications) { this.notifications = notifications; }
    }

    public static class NotificationPreferences implements Serializable {
        private boolean vaccines = true;
        private boolean deworming = true;
        private boolean medicalAppointments = true;
        private boolean grooming = true;
        private boolean feeding = true;
        private boolean walks = true;

        public boolean isVaccines() { return vaccines; }
        public void setVaccines(boolean vaccines) { this.vaccines = vaccines; }
        public boolean isDeworming() { return deworming; }
        public void setDeworming(boolean deworming) { this.deworming = deworming; }
        public boolean isMedicalAppointments() { return medicalAppointments; }
        public void setMedicalAppointments(boolean medicalAppointments) { this.medicalAppointments = medicalAppointments; }
        public boolean isGrooming() { return grooming; }
        public void setGrooming(boolean grooming) { this.grooming = grooming; }
        public boolean isFeeding() { return feeding; }
        public void setFeeding(boolean feeding) { this.feeding = feeding; }
        public boolean isWalks() { return walks; }
        public void setWalks(boolean walks) { this.walks = walks; }
    }
}
