package com.yolka.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yolka.app.BuildConfig;
import com.yolka.app.models.NotificationItem;
import com.yolka.app.models.Pet;
import com.yolka.app.models.Tip;
import com.yolka.app.models.User;
import com.yolka.app.models.VetClinic;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Store Singleton (inspirado en store.tsx de Kafoo):
 * - Almacena credenciales, tokens y usuario cifrado con EncryptedSharedPreferences + AES.
 * - Mantiene caché JSON cifrada de Mascotas, Veterinarias, Consejos y Notificaciones para operación parcialmente offline.
 * - Persiste la cola FIFO de sincronización (SyncQueueItem) para despachar cambios al restablecer la conexión.
 */
public class Store {

    private static final String PREFS_FILE = "yolka_secure_store";
    private static final String KEY_USER = "encrypted_user";
    private static final String KEY_TOKEN = "user_access_token";
    private static final String KEY_REFRESH_TOKEN = "user_refresh_token";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_DARK_MODE = "theme_dark_mode";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_TIMEZONE = "app_timezone";
    private static final String KEY_LAST_BACKUP = "last_backup_at";
    private static final String KEY_SELECTED_PET_ID = "selected_pet_id";

    // Claves de Caché Parcialmente Offline (JSON cifrado, sin SQLite)
    private static final String KEY_CACHED_PETS = "cached_pets_json";
    private static final String KEY_CACHED_VETS = "cached_vets_json";
    private static final String KEY_CACHED_TIPS = "cached_tips_json";
    private static final String KEY_CACHED_NOTIFICATIONS = "cached_notifications_json";
    private static final String KEY_PENDING_SYNC_QUEUE = "pending_sync_queue_json";

    private static volatile Store instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final String defaultDeviceId;

    private Store(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences initializedPrefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            initializedPrefs = EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            initializedPrefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
        this.prefs = initializedPrefs;

        String androidId = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.defaultDeviceId = (androidId != null && !androidId.isBlank())
                ? "android-" + androidId
                : "android-" + UUID.randomUUID().toString();
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (Store.class) {
                if (instance == null) {
                    instance = new Store(context);
                }
            }
        }
    }

    public static Store getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Store must be initialized in YolkaApplication first");
        }
        return instance;
    }

    // --- Usuario y Tokens ---

    public void storeUser(User user) {
        if (user == null) {
            prefs.edit().remove(KEY_USER).apply();
            return;
        }
        String json = gson.toJson(user);
        prefs.edit().putString(KEY_USER, encryptAes(json)).apply();
        if (user.getPreferences() != null) {
            setDarkMode(user.getPreferences().isDarkMode());
            setLanguage(user.getPreferences().getLanguage());
            setTimeZone(user.getPreferences().getTimeZone());
        }
        if (user.getLastDeviceBackupAt() != null) {
            setLastBackupAt(user.getLastDeviceBackupAt());
        }
    }

    public User getUser() {
        String encrypted = prefs.getString(KEY_USER, null);
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            String json = decryptAes(encrypted);
            return gson.fromJson(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void storeUserToken(String token) {
        if (token == null || token.isBlank()) {
            prefs.edit().remove(KEY_TOKEN).apply();
        } else {
            prefs.edit().putString(KEY_TOKEN, token.trim()).apply();
        }
    }

    public String getUserToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void storeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            prefs.edit().remove(KEY_REFRESH_TOKEN).apply();
        } else {
            prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken.trim()).apply();
        }
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getDeviceId() {
        String saved = prefs.getString(KEY_DEVICE_ID, null);
        if (saved == null || saved.isBlank()) {
            prefs.edit().putString(KEY_DEVICE_ID, defaultDeviceId).apply();
            return defaultDeviceId;
        }
        return saved;
    }

    public void storeFcmToken(String fcmToken) {
        if (fcmToken != null && !fcmToken.isBlank()) {
            prefs.edit().putString(KEY_FCM_TOKEN, fcmToken.trim()).apply();
        }
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, "yolka-mobile-fcm-token");
    }

    // --- Preferencias Locales de Sesión ---

    public void setDarkMode(boolean darkMode) {
        prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language != null ? language : "es").apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "es");
    }

    public void setTimeZone(String timeZone) {
        prefs.edit().putString(KEY_TIMEZONE, timeZone != null ? timeZone : "America/Mexico_City").apply();
    }

    public String getTimeZone() {
        return prefs.getString(KEY_TIMEZONE, "America/Mexico_City");
    }

    public void setLastBackupAt(String isoTimestamp) {
        prefs.edit().putString(KEY_LAST_BACKUP, isoTimestamp).apply();
    }

    public String getLastBackupAt() {
        return prefs.getString(KEY_LAST_BACKUP, null);
    }

    public void setSelectedPetId(String petId) {
        prefs.edit().putString(KEY_SELECTED_PET_ID, petId).apply();
    }

    public String getSelectedPetId() {
        return prefs.getString(KEY_SELECTED_PET_ID, null);
    }

    // --- Caché JSON Cifrada (Modo Parcialmente Offline) ---

    public void storeCachedPets(List<Pet> pets) {
        String json = gson.toJson(pets != null ? pets : new ArrayList<>());
        prefs.edit().putString(KEY_CACHED_PETS, encryptAes(json)).apply();
    }

    public List<Pet> getCachedPets() {
        String raw = prefs.getString(KEY_CACHED_PETS, null);
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Pet>>() {}.getType();
            List<Pet> list = gson.fromJson(decryptAes(raw), type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void upsertCachedPet(Pet pet) {
        if (pet == null) return;
        List<Pet> current = getCachedPets();
        boolean replaced = false;
        for (int i = 0; i < current.size(); i++) {
            if (pet.getId() != null && pet.getId().equals(current.get(i).getId())) {
                current.set(i, pet);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            current.add(0, pet);
        }
        storeCachedPets(current);
    }

    public void removeCachedPet(String petId) {
        if (petId == null) return;
        List<Pet> current = getCachedPets();
        current.removeIf(p -> petId.equals(p.getId()));
        storeCachedPets(current);
    }

    public void storeCachedVets(List<VetClinic> vets) {
        String json = gson.toJson(vets != null ? vets : new ArrayList<>());
        prefs.edit().putString(KEY_CACHED_VETS, encryptAes(json)).apply();
    }

    public List<VetClinic> getCachedVets() {
        String raw = prefs.getString(KEY_CACHED_VETS, null);
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<VetClinic>>() {}.getType();
            List<VetClinic> list = gson.fromJson(decryptAes(raw), type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void storeCachedTips(List<Tip> tips) {
        String json = gson.toJson(tips != null ? tips : new ArrayList<>());
        prefs.edit().putString(KEY_CACHED_TIPS, encryptAes(json)).apply();
    }

    public List<Tip> getCachedTips() {
        String raw = prefs.getString(KEY_CACHED_TIPS, null);
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Tip>>() {}.getType();
            List<Tip> list = gson.fromJson(decryptAes(raw), type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void storeCachedNotifications(List<NotificationItem> notifications) {
        String json = gson.toJson(notifications != null ? notifications : new ArrayList<>());
        prefs.edit().putString(KEY_CACHED_NOTIFICATIONS, encryptAes(json)).apply();
    }

    public List<NotificationItem> getCachedNotifications() {
        String raw = prefs.getString(KEY_CACHED_NOTIFICATIONS, null);
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<NotificationItem>>() {}.getType();
            List<NotificationItem> list = gson.fromJson(decryptAes(raw), type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- Cola de Sincronización Offline (SyncQueueItem) ---

    public synchronized void storePendingSyncQueue(List<SyncQueueManager.SyncQueueItem> queue) {
        String json = gson.toJson(queue != null ? queue : new ArrayList<>());
        prefs.edit().putString(KEY_PENDING_SYNC_QUEUE, encryptAes(json)).apply();
    }

    public synchronized List<SyncQueueManager.SyncQueueItem> getPendingSyncQueue() {
        String raw = prefs.getString(KEY_PENDING_SYNC_QUEUE, null);
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<SyncQueueManager.SyncQueueItem>>() {}.getType();
            List<SyncQueueManager.SyncQueueItem> list = gson.fromJson(decryptAes(raw), type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void clearSession() {
        boolean dark = isDarkMode();
        String lang = getLanguage();
        String tz = getTimeZone();
        String fcm = getFcmToken();
        prefs.edit().clear().apply();
        setDarkMode(dark);
        setLanguage(lang);
        setTimeZone(tz);
        storeFcmToken(fcm);
    }

    // --- Cifrado AES Auxiliar con BuildConfig.SECRET_KEY ---

    private SecretKeySpec deriveAesKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(BuildConfig.SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String encryptAes(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            return plainText;
        }
    }

    private String decryptAes(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, deriveAesKey());
            byte[] decoded = Base64.decode(cipherText, Base64.NO_WRAP);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }
}
