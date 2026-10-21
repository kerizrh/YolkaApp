package com.yolka.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.yolka.app.R;
import com.yolka.app.context.AuthManager;
import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.ActivityProfileSettingsBinding;
import com.yolka.app.models.ApiPayloads.BackupInfo;
import com.yolka.app.models.ApiPayloads.ChangePasswordRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.ApiPayloads.UpdateProfileRequest;
import com.yolka.app.models.User;
import com.yolka.app.services.BackupClientService;
import com.yolka.app.ui.auth.LoginActivity;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

/**
 * Módulo 9: Ajustes de Perfil, Seguridad y Respaldo en la Nube (.v2.ylk).
 * Implementa fielmente la Captura 11 de  (Modo Oscuro / Modo Claro).
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    private ActivityProfileSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        bindUserProfile();
        setupActions();
    }

    private void bindUserProfile() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user != null) {
            binding.tvProfileFullName.setText(user.getFullName() != null ? user.getFullName() : user.getFirstName());
            binding.tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            binding.etEditFullName.setText(user.getFullName() != null ? user.getFullName() : "");
            binding.etEditPhone.setText(user.getPhone() != null ? user.getPhone() : "");
            binding.etEditPhotoUrl.setText(user.getPhotoUrl() != null ? user.getPhotoUrl() : "");

            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isBlank()) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.bg_yolka_logo_badge)
                        .circleCrop()
                        .into(binding.ivProfileAvatar);
            }
        }

        String lastBackup = Store.getInstance().getLastBackupAt();
        if (lastBackup != null && !lastBackup.isBlank()) {
            binding.tvLastBackupTime.setText("Última copia: " + DateUtils.formatIsoDate(lastBackup));
            binding.tvBackupStatusBadge.setText("Sincronizado");
        } else {
            binding.tvLastBackupTime.setText("Sin copias recientes en este dispositivo");
            binding.tvBackupStatusBadge.setText("Pendiente");
        }
    }

    private void setupActions() {
        View.OnClickListener toggleEditProfile = v -> {
            int current = binding.cardEditProfileForm.getVisibility();
            binding.cardEditProfileForm.setVisibility(current == View.VISIBLE ? View.GONE : View.VISIBLE);
        };
        binding.btnToggleEditProfile.setOnClickListener(toggleEditProfile);
        binding.btnChangePhotoBadge.setOnClickListener(toggleEditProfile);

        binding.btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        binding.cardChangePasswordToggle.setOnClickListener(v -> {
            int current = binding.layoutPasswordForm.getVisibility();
            binding.layoutPasswordForm.setVisibility(current == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.btnSubmitPasswordChange.setOnClickListener(v -> submitPasswordChange());

        binding.btnCreateCloudBackup.setOnClickListener(v -> triggerCloudBackup());

        binding.btnRestoreCloudBackup.setOnClickListener(v -> triggerCloudRestore());

        binding.cardDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void saveProfileChanges() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null) return;

        String fullName = binding.etEditFullName.getText().toString().trim();
        String phone = binding.etEditPhone.getText().toString().trim();
        String photoUrl = binding.etEditPhotoUrl.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "El nombre completo no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setPhotoUrl(photoUrl);
        UserManager.getInstance().setUser(user);
        bindUserProfile();
        binding.cardEditProfileForm.setVisibility(View.GONE);

        UpdateProfileRequest req = new UpdateProfileRequest(fullName, phone, photoUrl, user.getPreferences());

        if (!NetworkMonitor.getInstance().isOnline()) {
            SyncQueueManager.getInstance().enqueue(
                    SyncQueueManager.OP_UPDATE_PROFILE,
                    user.getId(),
                    null,
                    req
            );
            Toast.makeText(this, "Perfil guardado localmente (se sincronizará al reconectar)", Toast.LENGTH_SHORT).show();
            return;
        }

        Api.getInstance().enqueue(
                Api.getInstance().getService().updateUserProfile(user.getId(), req),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        if (result.user != null) {
                            UserManager.getInstance().setUser(result.user);
                        }
                        Toast.makeText(ProfileSettingsActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        Toast.makeText(ProfileSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void submitPasswordChange() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) return;

        String currentPass = binding.etCurrentPassword.getText().toString();
        String newPass = binding.etNewPassword.getText().toString();

        if (currentPass.isEmpty() || newPass.length() < 8) {
            Toast.makeText(this, "Ingresa tu contraseña actual y una nueva de al menos 8 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitPasswordChange.setEnabled(false);
        Api.getInstance().enqueue(
                Api.getInstance().getService().changePassword(user.getId(), new ChangePasswordRequest(currentPass, newPass)),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        binding.btnSubmitPasswordChange.setEnabled(true);
                        binding.etCurrentPassword.setText("");
                        binding.etNewPassword.setText("");
                        binding.layoutPasswordForm.setVisibility(View.GONE);
                        Toast.makeText(ProfileSettingsActivity.this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        binding.btnSubmitPasswordChange.setEnabled(true);
                        Toast.makeText(ProfileSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void triggerCloudBackup() {
        binding.btnCreateCloudBackup.setEnabled(false);
        binding.btnCreateCloudBackup.setText("Cifrando...");

        BackupClientService.getInstance().createCloudBackup(new Api.ApiCallback<BackupInfo>() {
            @Override
            public void onSuccess(BackupInfo result) {
                binding.btnCreateCloudBackup.setEnabled(true);
                binding.btnCreateCloudBackup.setText("Crear copia");
                bindUserProfile();
                Toast.makeText(ProfileSettingsActivity.this, "Copia de seguridad (.v2.ylk) creada y sincronizada", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message, String errorCode) {
                binding.btnCreateCloudBackup.setEnabled(true);
                binding.btnCreateCloudBackup.setText("Crear copia");
                Toast.makeText(ProfileSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerCloudRestore() {
        BackupClientService.getInstance().restoreCloudBackup(new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                bindUserProfile();
                Toast.makeText(ProfileSettingsActivity.this, "Copia de seguridad restaurada desde Yolka Cloud", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message, String errorCode) {
                Toast.makeText(ProfileSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("¿Eliminar cuenta permanentemente?")
                .setMessage("Se borrarán tus datos personales, mascotas, historial clínico y respaldos en la nube. Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar cuenta", (dialog, which) -> executeAccountDeletion())
                .show();
    }

    private void executeAccountDeletion() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) return;

        Api.getInstance().enqueue(
                Api.getInstance().getService().deleteAccount(user.getId()),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        AuthManager.getInstance().logout(() -> {
                            Intent intent = new Intent(ProfileSettingsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        Toast.makeText(ProfileSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
