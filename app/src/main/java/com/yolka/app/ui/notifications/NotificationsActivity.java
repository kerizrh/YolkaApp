package com.yolka.app.ui.notifications;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.ActivityNotificationsBinding;
import com.yolka.app.models.ApiPayloads.CreateNotificationRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.NotificationItem;
import com.yolka.app.models.Pet;
import com.yolka.app.models.User;
import com.yolka.app.services.LocalReminderScheduler;
import com.yolka.app.ui.adapters.NotificationItemAdapter;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.Store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Módulo 8: Centro de Notificaciones Push y Recordatorios Locales.
 * Gestiona las preferencias por categoría (Vacunas, Desparasitación, Citas, Estética, Alimentación, Paseos)
 * y lista las alertas activas con soporte de caché offline.
 */
public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private NotificationItemAdapter adapter;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    sendTestNotification();
                } else {
                    Toast.makeText(this, "Permiso de notificaciones denegado por el sistema", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new NotificationItemAdapter();
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        loadPreferencesIntoSwitches();
        loadNotifications();

        binding.btnSavePreferences.setOnClickListener(v -> savePreferences());
        binding.btnTestPushNotification.setOnClickListener(v -> checkPermissionAndSendTest());
    }

    private void loadPreferencesIntoSwitches() {
        User user = UserManager.getInstance().getCurrentUser();
        User.NotificationPreferences notifPrefs = user != null
                ? user.getPreferences().getNotifications()
                : new User.NotificationPreferences();

        binding.switchVaccines.setChecked(notifPrefs.isVaccines());
        binding.switchDeworming.setChecked(notifPrefs.isDeworming());
        binding.switchMedicalAppointments.setChecked(notifPrefs.isMedicalAppointments());
        binding.switchGrooming.setChecked(notifPrefs.isGrooming());
        binding.switchFeeding.setChecked(notifPrefs.isFeeding());
        binding.switchWalks.setChecked(notifPrefs.isWalks());
    }

    private void savePreferences() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null) return;

        User.UserPreferences prefs = user.getPreferences();
        User.NotificationPreferences notif = prefs.getNotifications();
        notif.setVaccines(binding.switchVaccines.isChecked());
        notif.setDeworming(binding.switchDeworming.isChecked());
        notif.setMedicalAppointments(binding.switchMedicalAppointments.isChecked());
        notif.setGrooming(binding.switchGrooming.isChecked());
        notif.setFeeding(binding.switchFeeding.isChecked());
        notif.setWalks(binding.switchWalks.isChecked());
        prefs.setNotifications(notif);

        UserManager.getInstance().syncPreferences(prefs, new Api.ApiCallback<User>() {
            @Override
            public void onSuccess(User result) {
                Toast.makeText(NotificationsActivity.this, "Preferencias de notificaciones actualizadas", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message, String errorCode) {
                Toast.makeText(NotificationsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissionAndSendTest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        sendTestNotification();
    }

    private void sendTestNotification() {
        List<Pet> pets = Store.getInstance().getCachedPets();
        String petName = !pets.isEmpty() ? pets.get(0).getName() : "Max";
        String petId = !pets.isEmpty() ? pets.get(0).getId() : null;

        String title = "Recordatorio Yolka • " + petName;
        String body = "Revisión de rutina diaria y calendario de vacunación al día.";

        // Dispara notificación en barra de estado inmediatamente
        LocalReminderScheduler.triggerImmediateNotification(this, title, body);

        User currentUser = UserManager.getInstance().getCurrentUser();
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.userId = currentUser != null ? currentUser.getId() : null;
        req.petId = petId;
        req.petName = petName;
        req.category = "VACCINE";
        req.title = title;
        req.body = body;
        req.scheduledTime = "09:00";
        req.timeZone = Store.getInstance().getTimeZone();
        req.frequencyUnit = "DAYS";
        req.frequencyInterval = 1;
        req.daysOfWeek = Arrays.asList("L", "M", "X", "J", "V");

        Api.getInstance().enqueue(
                Api.getInstance().getService().createNotification(req),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        if (result.notification != null) {
                            List<NotificationItem> cached = Store.getInstance().getCachedNotifications();
                            cached.add(0, result.notification);
                            Store.getInstance().storeCachedNotifications(cached);
                            adapter.submitList(cached);
                        }
                        Toast.makeText(NotificationsActivity.this, "Notificación enviada y registrada", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        NotificationItem localItem = new NotificationItem();
                        localItem.setTitle(title);
                        localItem.setBody(body);
                        localItem.setCategory("VACCINE");
                        localItem.setPetName(petName);
                        localItem.setScheduledTime("09:00");
                        List<NotificationItem> cached = Store.getInstance().getCachedNotifications();
                        cached.add(0, localItem);
                        Store.getInstance().storeCachedNotifications(cached);
                        adapter.submitList(cached);
                    }
                }
        );
    }

    private void loadNotifications() {
        List<NotificationItem> cached = Store.getInstance().getCachedNotifications();
        if (!cached.isEmpty()) {
            adapter.submitList(cached);
        }

        User user = UserManager.getInstance().getCurrentUser();
        String userId = user != null ? user.getId() : null;

        Api.getInstance().enqueue(
                Api.getInstance().getService().getNotifications(userId),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        List<NotificationItem> list = result.notifications != null ? result.notifications : new ArrayList<>();
                        if (!list.isEmpty()) {
                            Store.getInstance().storeCachedNotifications(list);
                            adapter.submitList(list);
                        } else if (cached.isEmpty()) {
                            seedDefaultReminders();
                        }
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        if (cached.isEmpty()) {
                            seedDefaultReminders();
                        }
                    }
                }
        );
    }

    private void seedDefaultReminders() {
        List<NotificationItem> defaults = new ArrayList<>();
        NotificationItem n1 = new NotificationItem();
        n1.setTitle("Refuerzo Vacuna Rabia");
        n1.setBody("Próxima cita anual de refuerzo programada");
        n1.setCategory("VACCINE");
        n1.setPetName("Max");
        n1.setScheduledTime("09:00");
        defaults.add(n1);

        NotificationItem n2 = new NotificationItem();
        n2.setTitle("Desparasitación Trimestral");
        n2.setBody("Tableta masticable Simparica Trio");
        n2.setCategory("DEWORMING");
        n2.setPetName("Max");
        n2.setScheduledTime("10:00");
        defaults.add(n2);

        Store.getInstance().storeCachedNotifications(defaults);
        adapter.submitList(defaults);
    }
}
