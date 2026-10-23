package com.yolka.app.context;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.yolka.app.utils.Store;

import java.util.Locale;

/**
 * TranslationManager:
 * Administra el idioma ("es" o "en") y la zona horaria del usuario.
 */
public class TranslationManager {

    private static volatile TranslationManager instance;

    private TranslationManager() {}

    public static TranslationManager getInstance() {
        if (instance == null) {
            synchronized (TranslationManager.class) {
                if (instance == null) {
                    instance = new TranslationManager();
                }
            }
        }
        return instance;
    }

    public String getLanguage() {
        return Store.getInstance().getLanguage();
    }

    public void setLanguage(String languageCode) {
        String code = (languageCode != null && languageCode.toLowerCase(Locale.ROOT).startsWith("en")) ? "en" : "es";
        Store.getInstance().setLanguage(code);
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(code);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    public String getTimeZone() {
        return Store.getInstance().getTimeZone();
    }

    public void setTimeZone(String timeZone) {
        if (timeZone != null && !timeZone.isBlank()) {
            Store.getInstance().setTimeZone(timeZone.trim());
        }
    }

    public Context applyLocale(Context context) {
        String lang = getLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}
