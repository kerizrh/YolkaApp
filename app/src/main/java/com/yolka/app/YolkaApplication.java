package com.yolka.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import org.osmdroid.config.Configuration;

public class YolkaApplication extends Application {

    public static final String CHANNEL_ID_REMINDERS = "yolka-notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        Store.init(this);
        NetworkMonitor.init(this);
        createNotificationChannels();

        boolean darkMode = Store.getInstance().isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        Configuration.getInstance().setUserAgentValue(getPackageName());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
