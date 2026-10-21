package com.yolka.app.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.yolka.app.R;
import com.yolka.app.YolkaApplication;
import com.yolka.app.ui.dashboard.MainActivity;

/**
 * Receptor de alarmas locales (AlarmManager) para recordatorios programados de vacunas,
 * desparasitación, citas médicas, estética, alimentación y paseos (opera incluso sin red).
 */
public class ReminderAlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) (System.currentTimeMillis() & 0xfffffff));

        if (title == null || title.isBlank()) {
            title = "Recordatorio Yolka";
        }
        if (body == null || body.isBlank()) {
            body = "Es hora de cuidar a tu mascota.";
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, YolkaApplication.CHANNEL_ID_REMINDERS)
                .setSmallIcon(R.drawable.ic_yolka_paw)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }
}
