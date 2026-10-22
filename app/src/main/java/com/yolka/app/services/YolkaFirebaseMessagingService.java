package com.yolka.app.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.yolka.app.R;
import com.yolka.app.YolkaApplication;
import com.yolka.app.context.UserManager;
import com.yolka.app.models.ApiPayloads.FcmTokenRequest;
import com.yolka.app.models.ReceivedNotification;
import com.yolka.app.models.User;
import com.yolka.app.ui.dashboard.MainActivity;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.Store;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio FCM (Módulo 8):
 * Recibe mensajes push de Firebase Cloud Messaging, sincroniza el token FCM con el backend
 * (PUT /api/v1/users/{id}/fcm-token) y dispara notificaciones en el canal yolka-notifications.
 */
public class YolkaFirebaseMessagingService extends FirebaseMessagingService {

    private static final AtomicInteger NOTIFICATION_ID = new AtomicInteger(2000);

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Store.getInstance().storeFcmToken(token);
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            FcmTokenRequest req = new FcmTokenRequest(Store.getInstance().getDeviceId(), token);
            Api.getInstance().enqueue(
                    Api.getInstance().getService().updateFcmToken(currentUser.getId(), req),
                    null
            );
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = getString(R.string.app_name);
        String body = "Tienes un recordatorio pendiente para tu mascota";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        } else if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }

        String category = "GENERAL";
        String petName = "";
        if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("category")) {
                category = remoteMessage.getData().get("category");
            }
            if (remoteMessage.getData().containsKey("petName")) {
                petName = remoteMessage.getData().get("petName");
            }
        }
        Store.getInstance().addReceivedNotification(new ReceivedNotification(title, body, category, petName));

        showSystemNotification(title, body);
    }

    private void showSystemNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, YolkaApplication.CHANNEL_ID_REMINDERS)
                .setSmallIcon(R.drawable.ic_yolka_paw)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID.incrementAndGet(), builder.build());
    }
}
