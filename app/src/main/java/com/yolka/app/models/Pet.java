package com.yolka.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Pet implements Serializable {
    private String id;
    private String uuid;
    private String userId;
    private String name;
    private String species; // DOG | CAT
    private String breed;
    private String color;
    private String gender; // MALE | FEMALE
    private String birthDate; // YYYY-MM-DD
    private Integer ageYears;
    private Double weight;
    private String size; // SMALL | MEDIUM | LARGE
    private String photoUrl;
    private List<VaccineRecord> vaccines = new ArrayList<>();
    private List<DewormingRecord> dewormings = new ArrayList<>();
    private List<AppointmentRecord> appointments = new ArrayList<>();
    private List<RoutineRecord> routines = new ArrayList<>();
    private List<ReminderRecord> reminders = new ArrayList<>();
    private boolean deleted;
    private boolean pendingSync;

    public Pet() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }
    public String getSpecies() { return species != null ? species : "DOG"; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed != null ? breed : ""; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getColor() { return color != null ? color : ""; }
    public void setColor(String color) { this.color = color; }
    public String getGender() { return gender != null ? gender : "MALE"; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBirthDate() { return birthDate != null ? birthDate : ""; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public Integer getAgeYears() { return ageYears != null ? ageYears : 0; }
    public void setAgeYears(Integer ageYears) { this.ageYears = ageYears; }
    public Double getWeight() { return weight != null ? weight : 0.0; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getSize() { return size != null ? size : "MEDIUM"; }
    public void setSize(String size) { this.size = size; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<VaccineRecord> getVaccines() {
        if (vaccines == null) vaccines = new ArrayList<>();
        return vaccines;
    }
    public void setVaccines(List<VaccineRecord> vaccines) { this.vaccines = vaccines; }

    public List<DewormingRecord> getDewormings() {
        if (dewormings == null) dewormings = new ArrayList<>();
        return dewormings;
    }
    public void setDewormings(List<DewormingRecord> dewormings) { this.dewormings = dewormings; }

    public List<AppointmentRecord> getAppointments() {
        if (appointments == null) appointments = new ArrayList<>();
        return appointments;
    }
    public void setAppointments(List<AppointmentRecord> appointments) { this.appointments = appointments; }

    public List<RoutineRecord> getRoutines() {
        if (routines == null) routines = new ArrayList<>();
        return routines;
    }
    public void setRoutines(List<RoutineRecord> routines) { this.routines = routines; }

    public List<ReminderRecord> getReminders() {
        if (reminders == null) reminders = new ArrayList<>();
        return reminders;
    }
    public void setReminders(List<ReminderRecord> reminders) { this.reminders = reminders; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public boolean isPendingSync() { return pendingSync; }
    public void setPendingSync(boolean pendingSync) { this.pendingSync = pendingSync; }

    public static class VaccineRecord implements Serializable {
        private String id;
        private String vaccineName;
        private String applicationDate;
        private String nextDueDate;
        private String notes;
        private boolean deleted;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getVaccineName() { return vaccineName != null ? vaccineName : ""; }
        public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
        public String getApplicationDate() { return applicationDate != null ? applicationDate : ""; }
        public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }
        public String getNextDueDate() { return nextDueDate != null ? nextDueDate : ""; }
        public void setNextDueDate(String nextDueDate) { this.nextDueDate = nextDueDate; }
        public String getNotes() { return notes != null ? notes : ""; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }

    public static class DewormingRecord implements Serializable {
        private String id;
        private String type; // INTERNAL | EXTERNAL
        private String productName;
        private String applicationDate;
        private String nextDueDate;
        private String notes;
        private boolean deleted;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type != null ? type : "INTERNAL"; }
        public void setType(String type) { this.type = type; }
        public String getProductName() { return productName != null ? productName : ""; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getApplicationDate() { return applicationDate != null ? applicationDate : ""; }
        public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }
        public String getNextDueDate() { return nextDueDate != null ? nextDueDate : ""; }
        public void setNextDueDate(String nextDueDate) { this.nextDueDate = nextDueDate; }
        public String getNotes() { return notes != null ? notes : ""; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }

    public static class AppointmentRecord implements Serializable {
        private String id;
        private String clinicName;
        private String veterinarianName;
        private String reason;
        private String dateTime;
        private String notes;
        private boolean completed;
        private boolean deleted;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getClinicName() { return clinicName != null ? clinicName : ""; }
        public void setClinicName(String clinicName) { this.clinicName = clinicName; }
        public String getVeterinarianName() { return veterinarianName != null ? veterinarianName : ""; }
        public void setVeterinarianName(String veterinarianName) { this.veterinarianName = veterinarianName; }
        public String getReason() { return reason != null ? reason : ""; }
        public void setReason(String reason) { this.reason = reason; }
        public String getDateTime() { return dateTime != null ? dateTime : ""; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }
        public String getNotes() { return notes != null ? notes : ""; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }

    public static class RoutineRecord implements Serializable {
        private String id;
        private String category; // FEEDING | WALK | GROOMING_BATH | GROOMING_NAILS
        private String title;
        private String time; // HH:mm
        private List<String> daysOfWeek = new ArrayList<>();
        private boolean enabled = true;
        private String notes;
        private boolean deleted;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCategory() { return category != null ? category : "FEEDING"; }
        public void setCategory(String category) { this.category = category; }
        public String getTitle() { return title != null ? title : ""; }
        public void setTitle(String title) { this.title = title; }
        public String getTime() { return time != null ? time : "08:00"; }
        public void setTime(String time) { this.time = time; }
        public List<String> getDaysOfWeek() {
            if (daysOfWeek == null) daysOfWeek = new ArrayList<>();
            return daysOfWeek;
        }
        public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getNotes() { return notes != null ? notes : ""; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }

    public static class ReminderRecord implements Serializable {
        private String id;
        private String type;
        private String title;
        private String dateTime;
        private boolean completed;
        private boolean deleted;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type != null ? type : "GENERAL"; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title != null ? title : ""; }
        public void setTitle(String title) { this.title = title; }
        public String getDateTime() { return dateTime != null ? dateTime : ""; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }
}
