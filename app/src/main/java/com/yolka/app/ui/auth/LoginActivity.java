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
        binding.tvConfirmPendingAccount.setOnClickListener(v -> showConfirmAccountDialog(
                binding.etLoginEmail.getText().toString().trim()
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
                    showConfirmAccountDialog(email);
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
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText etCode = new EditText(this);
        etCode.setHint("Código de 6 dígitos (ej. 123456)");
        etCode.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(etCode);

        EditText etEmail = new EditText(this);
        etEmail.setHint("Correo electrónico (para reenviar)");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (prefilledEmail != null && !prefilledEmail.isBlank()) {
            etEmail.setText(prefilledEmail);
        }
        container.addView(etEmail);

        new AlertDialog.Builder(this)
                .setTitle("Confirmar cuenta Yolka")
                .setMessage("Ingresa el código de 6 dígitos enviado a tu correo o haz clic en el enlace del correo.")
                .setView(container)
                .setPositiveButton("Confirmar código", (d, w) -> {
                    String code = etCode.getText().toString().trim();
                    if (code.isEmpty()) return;
                    Api.getInstance().enqueue(
                            Api.getInstance().getService().confirmAccount(new ConfirmAccountRequest(code)),
                            new Api.ApiCallback<GenericResponse>() {
                                @Override
                                public void onSuccess(GenericResponse result) {
                                    Toast.makeText(LoginActivity.this,
                                            result.message != null ? result.message : "¡Cuenta confirmada! Ahora puedes iniciar sesión.",
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String message, String errorCode) {
                                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .setNeutralButton("Reenviar correo", (d, w) -> {
                    String email = etEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Ingresa tu correo para reenviar el código", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Api.getInstance().enqueue(
                            Api.getInstance().getService().requestConfirmation(new EmailRequest(email)),
                            new Api.ApiCallback<GenericResponse>() {
                                @Override
                                public void onSuccess(GenericResponse result) {
                                    Toast.makeText(LoginActivity.this, "Correo de confirmación enviado", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String message, String errorCode) {
                                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setLoading(boolean loading) {
        binding.pbLoginLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLoginSubmit.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
    }
}
