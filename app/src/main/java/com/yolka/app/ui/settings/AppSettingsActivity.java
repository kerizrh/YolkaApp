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
import com.yolka.app.utils.Api;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

/**
 * Módulo 10: Configuración de la App (Modo Claro/Oscuro, Idioma ES/EN, Zona Horaria,
 * Permisos del Sistema Android, Estado de Sincronización Offline y Cierre de Sesión).
 */
public class AppSettingsActivity extends AppCompatActivity {

    private ActivityAppSettingsBinding binding;

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
        binding.switchAppDarkMode.setChecked(ThemeManager.getInstance().isDarkMode());

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
            if (isChecked != ThemeManager.getInstance().isDarkMode()) {
                ThemeManager.getInstance().setDarkMode(isChecked, true);
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

        binding.btnLogoutApp.setOnClickListener(v -> {
            AuthManager.getInstance().logout(() -> {
                Intent intent = new Intent(AppSettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void refreshPermissionsSummary() {
        boolean locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean notifGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        String text = "• Ubicación GPS (Veterinarias): " + (locationGranted ? "Concedido" : "Pendiente") + "\n"
                + "• Notificaciones Push (Recordatorios): " + (notifGranted ? "Concedido" : "Pendiente");
        binding.tvPermissionsSummary.setText(text);
    }

    private void refreshSyncQueueSummary() {
        int pending = Store.getInstance().getPendingSyncQueue().size();
        int cachedPets = Store.getInstance().getCachedPets().size();
        binding.tvSyncQueueStatus.setText("Caché cifrada: " + cachedPets + " mascota(s) • Cola pendiente: " + pending + " operación(es)");
    }
}
