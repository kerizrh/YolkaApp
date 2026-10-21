package com.yolka.app.models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ApiPayloads {

    // --- Auth Requests & Responses ---
    public static class RegisterRequest {
        public String fullName;
        public String email;
        public String password;
        public String confirmPassword;
        public String phone;
        public String deviceId;
        public String deviceToken;

        public RegisterRequest(String fullName, String email, String password, String confirmPassword, String phone, String deviceId, String deviceToken) {
            this.fullName = fullName;
            this.email = email;
            this.password = password;
            this.confirmPassword = confirmPassword;
            this.phone = phone;
            this.deviceId = deviceId;
            this.deviceToken = deviceToken;
        }
    }

    public static class LoginRequest {
        public String email;
        public String password;
        public String deviceId;
        public String deviceToken;

        public LoginRequest(String email, String password, String deviceId, String deviceToken) {
            this.email = email;
            this.password = password;
            this.deviceId = deviceId;
            this.deviceToken = deviceToken;
        }
    }

    public static class GoogleLoginRequest {
        public String idToken;
        public String authorizationCode;
        public String deviceId;
        public String deviceToken;

        public GoogleLoginRequest(String idToken, String authorizationCode, String deviceId, String deviceToken) {
            this.idToken = idToken;
            this.authorizationCode = authorizationCode;
            this.deviceId = deviceId;
            this.deviceToken = deviceToken;
        }
    }

    public static class ConfirmAccountRequest {
        public String token;
        public ConfirmAccountRequest(String token) { this.token = token; }
    }

    public static class EmailRequest {
        public String email;
        public EmailRequest(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        public String email;
        public String code;
        public String newPassword;
        public ResetPasswordRequest(String email, String code, String newPassword) {
            this.email = email;
            this.code = code;
            this.newPassword = newPassword;
        }
    }

    public static class RefreshTokenRequest {
        public String refreshToken;
        public String deviceId;
        public RefreshTokenRequest(String refreshToken, String deviceId) {
            this.refreshToken = refreshToken;
            this.deviceId = deviceId;
        }
    }

    public static class LogoutRequest {
        public String deviceId;
        public String refreshToken;
        public LogoutRequest(String deviceId, String refreshToken) {
            this.deviceId = deviceId;
            this.refreshToken = refreshToken;
        }
    }

    public static class AuthResponse {
        public boolean success;
        public User user;
        public String token;
        public String refreshToken;
        public String message;
        public String error;
        public String code;
    }

    // --- Generic & Entity Responses ---
    public static class GenericResponse {
        public boolean success;
        public String message;
        public String error;
        public String code;
        public User user;
        public Pet pet;
        public List<Pet> pets;
        public List<VetClinic> vets;
        public List<Tip> tips;
        public List<NotificationItem> notifications;
        public NotificationItem notification;
        public BackupInfo backupInfo;
        public Map<String, Object> data;
    }

    public static class BackupInfo implements Serializable {
        public String id;
        public String uuid;
        public String userId;
        public String fileName;
        public long fileSize;
        public String type;
        public String createdAt;
        public String updatedAt;
    }

    // --- Pet & Clinical Requests ---
    public static class CreatePetRequest implements Serializable {
        public String userId;
        public String name;
        public String species;
        public String breed;
        public String color;
        public String gender;
        public String birthDate;
        public Integer ageYears;
        public Double weight;
        public String size;
        public String photoUrl;
    }

    public static class VaccineRequest implements Serializable {
        public String vaccineName;
        public String applicationDate;
        public String nextDueDate;
        public String notes;

        public VaccineRequest(String vaccineName, String applicationDate, String nextDueDate, String notes) {
            this.vaccineName = vaccineName;
            this.applicationDate = applicationDate;
            this.nextDueDate = nextDueDate;
            this.notes = notes;
        }
    }

    public static class DewormingRequest implements Serializable {
        public String type;
        public String productName;
        public String applicationDate;
        public String nextDueDate;
        public String notes;

        public DewormingRequest(String type, String productName, String applicationDate, String nextDueDate, String notes) {
            this.type = type;
            this.productName = productName;
            this.applicationDate = applicationDate;
            this.nextDueDate = nextDueDate;
            this.notes = notes;
        }
    }

    public static class AppointmentRequest implements Serializable {
        public String clinicName;
        public String veterinarianName;
        public String reason;
        public String dateTime;
        public String notes;
        public Boolean completed;

        public AppointmentRequest(String clinicName, String veterinarianName, String reason, String dateTime, String notes, Boolean completed) {
            this.clinicName = clinicName;
            this.veterinarianName = veterinarianName;
            this.reason = reason;
            this.dateTime = dateTime;
            this.notes = notes;
            this.completed = completed;
        }
    }

    public static class RoutineRequest implements Serializable {
        public String category;
        public String title;
        public String time;
        public List<String> daysOfWeek;
        public Boolean enabled;
        public String notes;

        public RoutineRequest(String category, String title, String time, List<String> daysOfWeek, Boolean enabled, String notes) {
            this.category = category;
            this.title = title;
            this.time = time;
            this.daysOfWeek = daysOfWeek;
            this.enabled = enabled;
            this.notes = notes;
        }
    }

    public static class ReminderRequest implements Serializable {
        public String type;
        public String title;
        public String dateTime;
        public Boolean completed;

        public ReminderRequest(String type, String title, String dateTime, Boolean completed) {
            this.type = type;
            this.title = title;
            this.dateTime = dateTime;
            this.completed = completed;
        }
    }

    // --- User & Notification Requests ---
    public static class UpdateProfileRequest implements Serializable {
        public String fullName;
        public String phone;
        public String photoUrl;
        public User.UserPreferences preferences;

        public UpdateProfileRequest(String fullName, String phone, String photoUrl, User.UserPreferences preferences) {
            this.fullName = fullName;
            this.phone = phone;
            this.photoUrl = photoUrl;
            this.preferences = preferences;
        }
    }

    public static class ChangePasswordRequest {
        public String currentPassword;
        public String newPassword;

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }

    public static class FcmTokenRequest {
        public String deviceId;
        public String fcmToken;

        public FcmTokenRequest(String deviceId, String fcmToken) {
            this.deviceId = deviceId;
            this.fcmToken = fcmToken;
        }
    }

    public static class CreateNotificationRequest implements Serializable {
        public String userId;
        public String petId;
        public String petName;
        public String category;
        public String title;
        public String body;
        public String scheduledTime;
        public String timeZone;
        public String frequencyUnit;
        public Integer frequencyInterval;
        public List<String> daysOfWeek;
    }
}
