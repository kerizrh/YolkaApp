package com.yolka.app.services;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.yolka.app.BuildConfig;

/**
 * Helper para Login con Google (Módulo 1):
 * Utiliza GOOGLE_WEB_CLIENT_ID inyectado desde local.properties vía BuildConfig
 * (sin ningún secreto hardcodeado en el código fuente).
 */
public class GoogleSignInHelper {

    public interface GoogleResultCallback {
        void onCredentialsObtained(String idToken, String serverAuthCode, String email, String displayName);
        void onError(String errorMessage);
    }

    private final GoogleSignInClient signInClient;

    public GoogleSignInHelper(Context context) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();

        if (BuildConfig.GOOGLE_WEB_CLIENT_ID != null && !BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            builder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim());
            builder.requestServerAuthCode(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim());
        }

        this.signInClient = GoogleSignIn.getClient(context, builder.build());
    }

    public Intent getSignInIntent() {
        return signInClient.getSignInIntent();
    }

    public void signOut() {
        signInClient.signOut();
    }

    public void handleSignInResult(Intent data, GoogleResultCallback callback) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                callback.onCredentialsObtained(
                        account.getIdToken(),
                        account.getServerAuthCode(),
                        account.getEmail(),
                        account.getDisplayName()
                );
            } else {
                callback.onError("No se pudo obtener la cuenta de Google.");
            }
        } catch (ApiException e) {
            callback.onError("Inicio de sesión con Google cancelado o no disponible (código " + e.getStatusCode() + "). Verifica la huella SHA-1 del emulador en Google Cloud.");
        }
    }
}
