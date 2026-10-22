package com.yolka.app.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.yolka.app.context.AuthManager;
import com.yolka.app.context.ThemeManager;
import com.yolka.app.context.TranslationManager;
import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.ActivityAppSettingsBinding;
import com.yolka.app.models.User;
import com.yolka.app.ui.auth.LoginActivity;
import com.yolka.app.ui.dialogs.PermissionsBottomSheetDialogFragment;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.PermissionUtils;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

/**
 * Módulo 10: Configuración de la App (Modo Claro/Oscuro, Idioma ES/EN, Zona Horaria,
 * Permisos del Sistema Android, Estado de Sincronización Offline y Cierre de Sesión).
 */
public class AppSettingsActivity extends AppCompatActivity {

    private ActivityAppSettingsBinding binding;
    private boolean isUpdatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        loadSettingsState();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionsSummary();
        refreshSyncQueueSummary();
    }

    private void loadSettingsState() {
        isUpdatingSwitch = true;
        binding.switchAppDarkMode.setChecked(ThemeManager.getInstance().isDarkMode());
        isUpdatingSwitch = false;

        String lang = TranslationManager.getInstance().getLanguage();
        if ("en".equalsIgnoreCase(lang)) {
            binding.rbEnglish.setChecked(true);
        } else {
            binding.rbSpanish.setChecked(true);
        }

        binding.etTimeZone.setText(TranslationManager.getInstance().getTimeZone());
        refreshPermissionsSummary();
        refreshSyncQueueSummary();
    }

    private void setupListeners() {
        binding.switchAppDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch) {
                return;
            }
            if (isChecked != ThemeManager.getInstance().isDarkMode()) {
                ThemeManager.getInstance().setDarkMode(isChecked, true);
                Toast.makeText(this,
                        isChecked ? "Modo oscuro activado" : "Modo claro activado",
                        Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSaveRegionalSettings.setOnClickListener(v -> {
            String lang = binding.rbEnglish.isChecked() ? "en" : "es";
            String tz = binding.etTimeZone.getText().toString().trim();
            if (tz.isEmpty()) tz = "America/Mexico_City";

            TranslationManager.getInstance().setTimeZone(tz);

            User user = UserManager.getInstance().getCurrentUser();
            if (user != null) {
                User.UserPreferences prefs = user.getPreferences();
                prefs.setLanguage(lang);
                prefs.setTimeZone(tz);
                prefs.setDarkMode(ThemeManager.getInstance().isDarkMode());
                UserManager.getInstance().syncPreferences(prefs, new Api.ApiCallback<User>() {
                    @Override
                    public void onSuccess(User result) {
                        Toast.makeText(AppSettingsActivity.this, "Preferencias regionales actualizadas", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        Toast.makeText(AppSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            TranslationManager.getInstance().setLanguage(lang);
        });

        binding.tvPermissionsSummary.setOnClickListener(v -> {
            if (!PermissionUtils.hasAllEssentialPermissions(this)) {
                PermissionsBottomSheetDialogFragment dialog = PermissionsBottomSheetDialogFragment.newInstance();
                dialog.setOnPermissionsResolvedListener(this::refreshPermissionsSummary);
                dialog.show(getSupportFragmentManager(), "PermissionsBottomSheet");
            } else {
                Toast.makeText(this, "Todos los permisos esenciales están concedidos", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnOpenSystemAppSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        binding.btnForceSyncNow.setOnClickListener(v -> {
            SyncQueueManager.getInstance().flushPendingQueue(syncedCount -> runOnUiThread(() -> {
                refreshSyncQueueSummary();
                Toast.makeText(AppSettingsActivity.this,
                        syncedCount > 0 ? "Se sincronizaron " + syncedCount + " cambios con Yolka Cloud" : "Todo está sincronizado",
                        Toast.LENGTH_SHORT).show();
            }));
        });
    }

    private void refreshPermissionsSummary() {
        boolean locationGranted = PermissionUtils.hasLocationPermission(this);
        boolean notifGranted = PermissionUtils.hasNotificationPermission(this);
        String text = "• Ubicación GPS (Veterinarias): " + (locationGranted ? "Concedido ✓" : "Pendiente ⚠️ (Toca para activar)") + "\n"
                + "• Notificaciones Push (Recordatorios): " + (notifGranted ? "Concedido ✓" : "Pendiente ⚠️ (Toca para activar)");
        binding.tvPermissionsSummary.setText(text);
    }

    private void refreshSyncQueueSummary() {
        int pending = Store.getInstance().getPendingSyncQueue().size();
        int cachedPets = Store.getInstance().getCachedPets().size();
        binding.tvSyncQueueStatus.setText("Caché cifrada: " + cachedPets + " mascota(s) • Cola pendiente: " + pending + " operación(es)");
    }
}
