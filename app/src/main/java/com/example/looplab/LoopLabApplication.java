package com.example.looplab;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class LoopLabApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeTheme();
    }

    private void initializeTheme() {
        SharedPreferences preferences = getSharedPreferences("LoopLabSettings", MODE_PRIVATE);
        boolean darkModeEnabled = preferences.getBoolean("dark_mode_enabled", false);
        
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
