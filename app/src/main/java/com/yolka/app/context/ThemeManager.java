package com.yolka.app.context;

import android.os.Looper;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.ApiPayloads.UpdateProfileRequest;
import com.yolka.app.models.User;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

/**
 * ThemeManager:
 * Aplica en caliente el Modo Claro y Modo Oscuro de la aplicación y sincroniza la preferencia
 * con el backend (o la encola si está sin conexión).
 */
public class ThemeManager {

    private static volatile ThemeManager instance;
    private final MutableLiveData<Boolean> darkModeLiveData = new MutableLiveData<>();

    private ThemeManager() {
        darkModeLiveData.setValue(Store.getInstance().isDarkMode());
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    public LiveData<Boolean> getDarkModeState() {
        return darkModeLiveData;
    }

    public boolean isDarkMode() {
        return Store.getInstance().isDarkMode();
    }

    public void setDarkMode(boolean enabled, boolean syncRemote) {
        Store.getInstance().setDarkMode(enabled);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            darkModeLiveData.setValue(enabled);
        } else {
            darkModeLiveData.postValue(enabled);
        }
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUser.getPreferences().setDarkMode(enabled);
            UserManager.getInstance().setUser(currentUser);
            if (syncRemote && currentUser.getId() != null) {
                UpdateProfileRequest req = new UpdateProfileRequest(
                        currentUser.getFullName(),
                        currentUser.getPhone(),
                        currentUser.getPhotoUrl(),
                        currentUser.getPreferences()
                );
                if (NetworkMonitor.getInstance().isOnline()) {
                    Api.getInstance().enqueue(
                            Api.getInstance().getService().updateUserProfile(currentUser.getId(), req),
                            new Api.ApiCallback<GenericResponse>() {
                                @Override
                                public void onSuccess(GenericResponse result) {
                                    if (result.user != null) {
                                        UserManager.getInstance().setUser(result.user);
                                    }
                                }

                                @Override
                                public void onError(String message, String errorCode) {
                                    SyncQueueManager.getInstance().enqueue(
                                            SyncQueueManager.OP_UPDATE_PROFILE,
                                            currentUser.getId(),
                                            null,
                                            req
                                    );
                                }
                            }
                    );
                } else {
                    SyncQueueManager.getInstance().enqueue(
                            SyncQueueManager.OP_UPDATE_PROFILE,
                            currentUser.getId(),
                            null,
                            req
                    );
                }
            }
        }
    }
}
