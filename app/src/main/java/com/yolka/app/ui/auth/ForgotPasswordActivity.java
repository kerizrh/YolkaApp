package com.yolka.app.ui.auth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yolka.app.databinding.ActivityForgotPasswordBinding;
import com.yolka.app.models.ApiPayloads.EmailRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.ApiPayloads.ResetPasswordRequest;
import com.yolka.app.utils.Api;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvCancelForgot.setOnClickListener(v -> finish());

        binding.btnSendRecoveryCode.setOnClickListener(v -> {
            String email = binding.etRecoverEmail.getText().toString().trim();
            if (email.isEmpty()) {
                binding.etRecoverEmail.setError("Ingresa tu correo electrónico");
                return;
            }
            binding.btnSendRecoveryCode.setEnabled(false);
            Api.getInstance().enqueue(
                    Api.getInstance().getService().recoverPassword(new EmailRequest(email)),
                    new Api.ApiCallback<GenericResponse>() {
                        @Override
                        public void onSuccess(GenericResponse result) {
                            binding.btnSendRecoveryCode.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this,
                                    result.message != null ? result.message : "Código enviado a tu correo",
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message, String errorCode) {
                            binding.btnSendRecoveryCode.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        binding.btnResetPasswordSubmit.setOnClickListener(v -> {
            String email = binding.etRecoverEmail.getText().toString().trim();
            String code = binding.etRecoveryCode.getText().toString().trim();
            String newPassword = binding.etNewPassword.getText().toString();

            if (email.isEmpty()) {
                binding.etRecoverEmail.setError("Ingresa tu correo");
                return;
            }
            if (code.isEmpty()) {
                binding.etRecoveryCode.setError("Ingresa el código de 6 dígitos");
                return;
            }
            if (newPassword.length() < 6) {
                binding.etNewPassword.setError("Mínimo 6 caracteres");
                return;
            }

            binding.btnResetPasswordSubmit.setEnabled(false);
            Api.getInstance().enqueue(
                    Api.getInstance().getService().resetPassword(new ResetPasswordRequest(email, code, newPassword)),
                    new Api.ApiCallback<GenericResponse>() {
                        @Override
                        public void onSuccess(GenericResponse result) {
                            binding.btnResetPasswordSubmit.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Contraseña restablecida exitosamente. Inicia sesión.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onError(String message, String errorCode) {
                            binding.btnResetPasswordSubmit.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });
    }
}
