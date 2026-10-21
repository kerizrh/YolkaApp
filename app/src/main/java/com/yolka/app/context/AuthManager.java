package com.yolka.app.context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.messaging.FirebaseMessaging;
import com.yolka.app.models.ApiPayloads.*;
import com.yolka.app.models.User;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

/**
 * AuthManager (inspirado en AuthContext de Kafoo):
 * Controla el estado de sesión (CHECKING, AUTHENTICATED, UNAUTHENTICATED),
 * login por correo y Google, registro, confirmación, recuperación y cierre de sesión.
 */
public class AuthManager {

    public enum AuthStatus {
        CHECKING,
        AUTHENTICATED,
        UNAUTHENTICATED
    }

    private static volatile AuthManager instance;
    private final MutableLiveData<AuthStatus> statusLiveData = new MutableLiveData<>(AuthStatus.CHECKING);

    private AuthManager() {}

    public static AuthManager getInstance() {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager();
                }
            }
        }
        return instance;
    }

    public LiveData<AuthStatus> getStatusLiveData() {
        return statusLiveData;
    }

    public void checkExistingSession(Api.ApiCallback<User> callback) {
        statusLiveData.postValue(AuthStatus.CHECKING);
        String token = Store.getInstance().getUserToken();
        User cachedUser = Store.getInstance().getUser();

        if (token == null || token.isBlank() || cachedUser == null || cachedUser.getId() == null) {
            statusLiveData.postValue(AuthStatus.UNAUTHENTICATED);
            if (callback != null) callback.onError("No hay sesión activa", "UNAUTHENTICATED");
            return;
        }

        // Modo Parcialmente Offline: si no hay red pero hay sesión válida guardada, entrar al Dashboard
        if (!NetworkMonitor.getInstance().isOnline()) {
            UserManager.getInstance().setUser(cachedUser);
            statusLiveData.postValue(AuthStatus.AUTHENTICATED);
            if (callback != null) callback.onSuccess(cachedUser);
            return;
        }

        Api.getInstance().enqueue(
                Api.getInstance().getService().getUserProfile(cachedUser.getId()),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        User freshUser = result.user != null ? result.user : cachedUser;
                        UserManager.getInstance().setUser(freshUser);
                        ThemeManager.getInstance().setDarkMode(
                                freshUser.getPreferences().isDarkMode(),
                                false
                        );
                        statusLiveData.postValue(AuthStatus.AUTHENTICATED);
                        SyncQueueManager.getInstance().flushPendingQueue(null);
                        if (callback != null) callback.onSuccess(freshUser);
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        if ("NETWORK_ERROR".equals(errorCode)) {
                            // Mantener sesión offline si fue fallo de red temporal
                            UserManager.getInstance().setUser(cachedUser);
                            statusLiveData.postValue(AuthStatus.AUTHENTICATED);
                            if (callback != null) callback.onSuccess(cachedUser);
                        } else {
                            Store.getInstance().clearSession();
                            statusLiveData.postValue(AuthStatus.UNAUTHENTICATED);
                            if (callback != null) callback.onError(message, errorCode);
                        }
                    }
                }
        );
    }

    public void fetchFcmTokenAndExecute(Runnable afterTokenFetched) {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Store.getInstance().storeFcmToken(task.getResult());
                }
                if (afterTokenFetched != null) afterTokenFetched.run();
            });
        } catch (Exception e) {
            if (afterTokenFetched != null) afterTokenFetched.run();
        }
    }

    public void login(String email, String password, Api.ApiCallback<AuthResponse> callback) {
        fetchFcmTokenAndExecute(() -> {
            LoginRequest req = new LoginRequest(
                    email.trim(),
                    password,
                    Store.getInstance().getDeviceId(),
                    Store.getInstance().getFcmToken()
            );
            Api.getInstance().enqueue(Api.getInstance().getService().login(req), new Api.ApiCallback<AuthResponse>() {
                @Override
                public void onSuccess(AuthResponse result) {
                    handleSuccessfulAuth(result);
                    if (callback != null) callback.onSuccess(result);
                }

                @Override
                public void onError(String message, String errorCode) {
                    if (callback != null) callback.onError(message, errorCode);
                }
            });
        });
    }

    public void register(String fullName, String email, String password, String confirmPassword, String phone, Api.ApiCallback<AuthResponse> callback) {
        fetchFcmTokenAndExecute(() -> {
            RegisterRequest req = new RegisterRequest(
                    fullName.trim(),
                    email.trim(),
                    password,
                    confirmPassword,
                    phone != null ? phone.trim() : "",
                    Store.getInstance().getDeviceId(),
                    Store.getInstance().getFcmToken()
            );
            Api.getInstance().enqueue(Api.getInstance().getService().register(req), new Api.ApiCallback<AuthResponse>() {
                @Override
                public void onSuccess(AuthResponse result) {
                    if (result.token != null && !result.token.isBlank()) {
                        handleSuccessfulAuth(result);
                    }
                    if (callback != null) callback.onSuccess(result);
                }

                @Override
                public void onError(String message, String errorCode) {
                    if (callback != null) callback.onError(message, errorCode);
                }
            });
        });
    }

    public void loginWithGoogle(String idToken, String authCode, Api.ApiCallback<AuthResponse> callback) {
        fetchFcmTokenAndExecute(() -> {
            GoogleLoginRequest req = new GoogleLoginRequest(
                    idToken,
                    authCode,
                    Store.getInstance().getDeviceId(),
                    Store.getInstance().getFcmToken()
            );
            Api.getInstance().enqueue(Api.getInstance().getService().googleSignIn(req), new Api.ApiCallback<AuthResponse>() {
                @Override
                public void onSuccess(AuthResponse result) {
                    handleSuccessfulAuth(result);
                    if (callback != null) callback.onSuccess(result);
                }

                @Override
                public void onError(String message, String errorCode) {
                    if (callback != null) callback.onError(message, errorCode);
                }
            });
        });
    }

    private void handleSuccessfulAuth(AuthResponse result) {
        if (result.token != null) {
            Store.getInstance().storeUserToken(result.token);
        }
        if (result.refreshToken != null) {
            Store.getInstance().storeRefreshToken(result.refreshToken);
        }
        if (result.user != null) {
            UserManager.getInstance().setUser(result.user);
            if (result.user.getPreferences() != null) {
                ThemeManager.getInstance().setDarkMode(result.user.getPreferences().isDarkMode(), false);
            }
        }
        statusLiveData.postValue(AuthStatus.AUTHENTICATED);
    }

    public void logout(Runnable onComplete) {
        LogoutRequest req = new LogoutRequest(
                Store.getInstance().getDeviceId(),
                Store.getInstance().getRefreshToken()
        );
        Api.getInstance().enqueue(Api.getInstance().getService().logout(req), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                finishLogout(onComplete);
            }

            @Override
            public void onError(String message, String errorCode) {
                finishLogout(onComplete);
            }
        });
    }

    private void finishLogout(Runnable onComplete) {
        Store.getInstance().clearSession();
        UserManager.getInstance().setUser(null);
        statusLiveData.postValue(AuthStatus.UNAUTHENTICATED);
        if (onComplete != null) onComplete.run();
    }
}
