package com.example.NeuraFuse.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class AppBlockAccessibilityService extends AccessibilityService {

    private Set<String> primaryApps;

    @Override
    public void onCreate() {
        super.onCreate();
        loadPrimaryApps();
    }

    private void loadPrimaryApps() {
        SharedPreferences prefs = getSharedPreferences("PrimaryApps", MODE_PRIVATE);
        primaryApps = prefs.getStringSet("primaryApps", new HashSet<>());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (!primaryApps.contains(packageName) && !packageName.equals(getPackageName())) {
            Log.d("BlockService", "Pushing back: " + packageName);
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onInterrupt() {
        // Required override
    }
}
