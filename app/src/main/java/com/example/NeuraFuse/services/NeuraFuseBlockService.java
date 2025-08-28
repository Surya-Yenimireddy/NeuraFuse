package com.example.NeuraFuse.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class NeuraFuseBlockService extends AccessibilityService {

    private Set<String> primaryApps;

    @Override
    public void onCreate() {
        super.onCreate();
        loadPrimaryApps();
    }

    private void loadPrimaryApps() {
        SharedPreferences prefs = getSharedPreferences("PrimaryApps", MODE_PRIVATE);
        primaryApps = prefs.getStringSet("primaryApps", new HashSet<>());
        Log.d("NeuraFuseBlock", "Loaded primary apps: " + primaryApps);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageNameCS = event.getPackageName();
            if (packageNameCS == null) return;

            String foregroundApp = packageNameCS.toString();
            String myAppPackage = "com.example.NeuraFuse"; // prevent self block

            if (!foregroundApp.equals(myAppPackage) && !primaryApps.contains(foregroundApp)) {
                Log.d("NeuraFuseBlock", "Blocking non-primary app: " + foregroundApp);

                // Push to background
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("NeuraFuseBlock", "Accessibility Service Interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        loadPrimaryApps();
        Log.d("NeuraFuseBlock", "Service connected");
    }
}
