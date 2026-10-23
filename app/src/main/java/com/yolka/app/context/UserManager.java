package com.yolka.app.context;

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
 * UserManager:
 * Mantiene en memoria y sincroniza el perfil del usuario autenticado y sus preferencias.
 */
public class UserManager {

    private static volatile UserManager instance;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();

    private UserManager() {
        User cached = Store.getInstance().getUser();
        if (cached != null) {
            userLiveData.setValue(cached);
        }
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public User getCurrentUser() {
        User current = userLiveData.getValue();
        if (current == null) {
            current = Store.getInstance().getUser();
        }
        return current;
    }

    public void setUser(User user) {
        Store.getInstance().storeUser(user);
        userLiveData.postValue(user);
    }

    public void syncPreferences(User.UserPreferences preferences, Api.ApiCallback<User> callback) {
        User user = getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("Sesión no encontrada", "NO_SESSION");
            return;
        }
        user.setPreferences(preferences);
        setUser(user);

        UpdateProfileRequest req = new UpdateProfileRequest(
                user.getFullName(),
                user.getPhone(),
                user.getPhotoUrl(),
                preferences
        );

        if (!NetworkMonitor.getInstance().isOnline()) {
            SyncQueueManager.getInstance().enqueue(
                    SyncQueueManager.OP_UPDATE_PROFILE,
                    user.getId(),
                    null,
                    req
            );
            if (callback != null) callback.onSuccess(user);
            return;
        }

        Api.getInstance().enqueue(
                Api.getInstance().getService().updateUserProfile(user.getId(), req),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        if (result.user != null) {
                            setUser(result.user);
                            if (callback != null) callback.onSuccess(result.user);
                        } else if (callback != null) {
                            callback.onSuccess(user);
                        }
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        if ("NETWORK_ERROR".equals(errorCode)) {
                            SyncQueueManager.getInstance().enqueue(
                                    SyncQueueManager.OP_UPDATE_PROFILE,
                                    user.getId(),
                                    null,
                                    req
                            );
                            if (callback != null) callback.onSuccess(user);
                        } else if (callback != null) {
                            callback.onError(message, errorCode);
                        }
                    }
                }
        );
    }
}
