package com.yolka.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yolka.app.context.AuthManager;
import com.yolka.app.databinding.ActivityLoginBinding;
import com.yolka.app.models.ApiPayloads.AuthResponse;
import com.yolka.app.models.ApiPayloads.ConfirmAccountRequest;
import com.yolka.app.models.ApiPayloads.EmailRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.services.GoogleSignInHelper;
import com.yolka.app.ui.dashboard.MainActivity;
import com.yolka.app.utils.Api;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private GoogleSignInHelper googleSignInHelper;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (googleSignInHelper != null) {
                    googleSignInHelper.handleSignInResult(result.getData(), new GoogleSignInHelper.GoogleResultCallback() {
                        @Override
                        public void onCredentialsObtained(String idToken, String serverAuthCode, String email, String displayName) {
                            setLoading(true);
                            AuthManager.getInstance().loginWithGoogle(idToken, serverAuthCode, new Api.ApiCallback<AuthResponse>() {
                                @Override
                                public void onSuccess(AuthResponse response) {
                                    setLoading(false);
                                    onAuthCompleted(response);
                                }

                                @Override
                                public void onError(String message, String errorCode) {
                                    setLoading(false);
                                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        googleSignInHelper = new GoogleSignInHelper(this);

        binding.btnLoginSubmit.setOnClickListener(v -> performEmailLogin());
        binding.btnGoogleSignIn.setOnClickListener(v -> googleLauncher.launch(googleSignInHelper.getSignInIntent()));
        binding.tvGoToSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.tvConfirmPendingAccount.setOnClickListener(v -> ConfirmAccountActivity.start(
                this,
                binding.etLoginEmail.getText().toString().trim(),
                binding.etLoginPassword.getText().toString()
        ));
    }

    private void performEmailLogin() {
        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString();

        if (email.isEmpty()) {
            binding.etLoginEmail.setError("Ingresa tu correo");
            return;
        }
        if (password.isEmpty()) {
            binding.etLoginPassword.setError("Ingresa tu contraseña");
            return;
        }

        setLoading(true);
        AuthManager.getInstance().login(email, password, new Api.ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse result) {
                setLoading(false);
                onAuthCompleted(result);
            }

            @Override
            public void onError(String message, String errorCode) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                if ("ACCOUNT_NOT_CONFIRMED".equals(errorCode)) {
                    ConfirmAccountActivity.start(LoginActivity.this, email, password);
                }
            }
        });
    }

    private void onAuthCompleted(AuthResponse response) {
        if (response.user != null && response.user.isHasDeviceBackup()) {
            RestoreBackupDialogFragment dialog = RestoreBackupDialogFragment.newInstance(
                    response.user.getLastDeviceBackupAt(),
                    restored -> navigateToDashboard()
            );
            dialog.show(getSupportFragmentManager(), "RestoreBackupDialog");
        } else {
            navigateToDashboard();
        }
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void showConfirmAccountDialog(String prefilledEmail) {
        ConfirmAccountActivity.start(this, prefilledEmail, null);
    }

    private void setLoading(boolean loading) {
        binding.pbLoginLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLoginSubmit.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
    }
}
