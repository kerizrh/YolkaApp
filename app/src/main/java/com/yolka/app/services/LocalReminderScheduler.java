package com.yolka.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Programador de recordatorios locales mediante AlarmManager.
 * Permite disparar notificaciones de prueba inmediatas o programadas (funciona 100% offline).
 */
public class LocalReminderScheduler {

    private LocalReminderScheduler() {}

    public static void scheduleReminder(Context context, int requestCode, String title, String body, long triggerAtMillis) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(appContext, ReminderAlarmReceiver.class);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_BODY, body);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } catch (SecurityException ignored) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void triggerImmediateNotification(Context context, String title, String body) {
        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_BODY, body);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_NOTIFICATION_ID, (int) (System.currentTimeMillis() & 0xfffffff));
        new ReminderAlarmReceiver().onReceive(context, intent);
    }
}
