package com.yolka.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import org.osmdroid.config.Configuration;

public class YolkaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Store.init(this);
        NetworkMonitor.init(this);

        boolean darkMode = Store.getInstance().isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        Configuration.getInstance().setUserAgentValue(getPackageName());
    }
}
