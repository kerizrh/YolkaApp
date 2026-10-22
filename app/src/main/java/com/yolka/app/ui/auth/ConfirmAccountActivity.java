package com.yolka.app.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yolka.app.context.AuthManager;
import com.yolka.app.databinding.ActivityConfirmAccountBinding;
import com.yolka.app.models.ApiPayloads.AuthResponse;
import com.yolka.app.models.ApiPayloads.ConfirmAccountRequest;
import com.yolka.app.models.ApiPayloads.EmailRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.ui.dashboard.MainActivity;
import com.yolka.app.utils.Api;

public class ConfirmAccountActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_PASSWORD = "extra_password";

    private ActivityConfirmAccountBinding binding;
    private String email;
    private String password;
    private CountDownTimer resendTimer;

    public static void start(Context context, String email, String password) {
        Intent intent = new Intent(context, ConfirmAccountActivity.class);
        intent.putExtra(EXTRA_EMAIL, email);
        if (password != null) {
            intent.putExtra(EXTRA_PASSWORD, password);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConfirmAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        password = getIntent().getStringExtra(EXTRA_PASSWORD);

        if (email != null && !email.isBlank()) {
            binding.tvConfirmSubtitle.setText("Hemos enviado un código de confirmación de 6 dígitos a:\n"
                    + email + "\n\nIngrésalo a continuación para activar tu cuenta.");
        }

        binding.btnConfirmAccount.setOnClickListener(v -> confirmCode());
        binding.btnResendCode.setOnClickListener(v -> resendCode());
        binding.btnBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmAccountActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void confirmCode() {
        String code = binding.etConfirmationCode.getText().toString().trim();
        if (code.isEmpty()) {
            binding.etConfirmationCode.setError("Ingresa el código de 6 dígitos");
            return;
        }
        if (code.length() < 6) {
            binding.etConfirmationCode.setError("El código debe tener 6 dígitos");
            return;
        }

        setLoading(true);
        Api.getInstance().enqueue(
                Api.getInstance().getService().confirmAccount(new ConfirmAccountRequest(code)),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        Toast.makeText(ConfirmAccountActivity.this, "¡Cuenta confirmada exitosamente! 🐾", Toast.LENGTH_SHORT).show();

                        if (password != null && !password.isBlank() && email != null && !email.isBlank()) {
                            AuthManager.getInstance().login(email, password, new Api.ApiCallback<AuthResponse>() {
                                @Override
                                public void onSuccess(AuthResponse loginResp) {
                                    setLoading(false);
                                    if (loginResp.user != null && loginResp.user.isHasDeviceBackup()) {
                                        RestoreBackupDialogFragment dialog = RestoreBackupDialogFragment.newInstance(
                                                loginResp.user.getLastDeviceBackupAt(),
                                                restored -> navigateToDashboard()
                                        );
                                        dialog.show(getSupportFragmentManager(), "RestoreBackupDialog");
                                    } else {
                                        navigateToDashboard();
                                    }
                                }

                                @Override
                                public void onError(String message, String errorCode) {
                                    setLoading(false);
                                    goToLoginWithSuccess();
                                }
                            });
                        } else {
                            setLoading(false);
                            goToLoginWithSuccess();
                        }
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        setLoading(false);
                        Toast.makeText(ConfirmAccountActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void resendCode() {
        if (email == null || email.isBlank()) {
            Toast.makeText(this, "Correo no disponible para reenvío", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnResendCode.setEnabled(false);
        Api.getInstance().enqueue(
                Api.getInstance().getService().requestConfirmation(new EmailRequest(email)),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        Toast.makeText(ConfirmAccountActivity.this,
                                "Se ha enviado un nuevo código de confirmación a " + email,
                                Toast.LENGTH_LONG).show();
                        startResendCooldown();
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        binding.btnResendCode.setEnabled(true);
                        Toast.makeText(ConfirmAccountActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void startResendCooldown() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        resendTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.btnResendCode.setText("Reenviar en " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                binding.btnResendCode.setEnabled(true);
                binding.btnResendCode.setText("Reenviar código de confirmación");
            }
        }.start();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLoginWithSuccess() {
        Toast.makeText(this, "Cuenta activada. Ya puedes iniciar sesión.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.pbConfirmLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnConfirmAccount.setEnabled(!loading);
        binding.btnResendCode.setEnabled(!loading);
        binding.etConfirmationCode.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        super.onDestroy();
    }
}
