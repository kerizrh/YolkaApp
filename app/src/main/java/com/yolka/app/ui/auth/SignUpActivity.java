package com.yolka.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yolka.app.context.AuthManager;
import com.yolka.app.databinding.ActivitySignUpBinding;
import com.yolka.app.models.ApiPayloads.AuthResponse;
import com.yolka.app.models.ApiPayloads.ConfirmAccountRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.ui.dashboard.MainActivity;
import com.yolka.app.utils.Api;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBackFromSignUp.setOnClickListener(v -> finish());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
        binding.btnSignUpSubmit.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String fullName = binding.etSignUpFullName.getText().toString().trim();
        String usernameOrPhone = binding.etSignUpUsernameOrPhone.getText().toString().trim();
        String email = binding.etSignUpEmail.getText().toString().trim();
        String password = binding.etSignUpPassword.getText().toString();
        String confirmPassword = binding.etSignUpConfirmPassword.getText().toString();

        if (fullName.isEmpty()) {
            binding.etSignUpFullName.setError("Ingresa tu nombre");
            return;
        }
        if (email.isEmpty()) {
            binding.etSignUpEmail.setError("Ingresa tu correo");
            return;
        }
        if (password.length() < 6) {
            binding.etSignUpPassword.setError("Mínimo 6 caracteres");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.etSignUpConfirmPassword.setError("Las contraseñas no coinciden");
            return;
        }

        setLoading(true);
        AuthManager.getInstance().register(fullName, email, password, confirmPassword, usernameOrPhone, new Api.ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse result) {
                setLoading(false);
                String msg = result.message != null ? result.message : "Registro exitoso. Confirma tu cuenta con el código enviado a tu correo.";
                Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_LONG).show();

                if (result.token != null && !result.token.isBlank()) {
                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    promptCodeConfirmation(email, password);
                }
            }

            @Override
            public void onError(String message, String errorCode) {
                setLoading(false);
                Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void promptCodeConfirmation(String email, String password) {
        EditText etCode = new EditText(this);
        etCode.setHint("Código de 6 dígitos");
        etCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        etCode.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Confirma tu correo")
                .setMessage("Hemos enviado un enlace y un código de 6 dígitos a " + email + ". Ingresa el código aquí para activar tu cuenta de inmediato:")
                .setView(etCode)
                .setCancelable(false)
                .setPositiveButton("Confirmar y Entrar", (d, w) -> {
                    String code = etCode.getText().toString().trim();
                    if (code.isEmpty()) {
                        finish();
                        return;
                    }
                    setLoading(true);
                    Api.getInstance().enqueue(
                            Api.getInstance().getService().confirmAccount(new ConfirmAccountRequest(code)),
                            new Api.ApiCallback<GenericResponse>() {
                                @Override
                                public void onSuccess(GenericResponse confirmResp) {
                                    AuthManager.getInstance().login(email, password, new Api.ApiCallback<AuthResponse>() {
                                        @Override
                                        public void onSuccess(AuthResponse loginResp) {
                                            setLoading(false);
                                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }

                                        @Override
                                        public void onError(String message, String errorCode) {
                                            setLoading(false);
                                            finish();
                                        }
                                    });
                                }

                                @Override
                                public void onError(String message, String errorCode) {
                                    setLoading(false);
                                    Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .setNegativeButton("Confirmaré desde mi correo", (d, w) -> finish())
                .show();
    }

    private void setLoading(boolean loading) {
        binding.pbSignUpLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSignUpSubmit.setEnabled(!loading);
    }
}
