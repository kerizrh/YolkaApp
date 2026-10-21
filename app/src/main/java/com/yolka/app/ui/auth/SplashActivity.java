package com.yolka.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.yolka.app.context.AuthManager;
import com.yolka.app.databinding.ActivitySplashBinding;
import com.yolka.app.models.User;
import com.yolka.app.ui.dashboard.MainActivity;
import com.yolka.app.utils.Api;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthManager.getInstance().checkExistingSession(new Api.ApiCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    if (isFinishing()) return;
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String message, String errorCode) {
                    if (isFinishing()) return;
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            });
        }, 500);
    }
}
