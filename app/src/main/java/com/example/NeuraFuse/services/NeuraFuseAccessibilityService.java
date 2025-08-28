package com.example.NeuraFuse.services;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
import android.util.Log;

public class NeuraFuseAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName = String.valueOf(event.getPackageName());

        // You can add logic here if needed
        Log.d("NeuraFuse-Accessibility", "Current App: " + packageName);
    }

    @Override
    public void onInterrupt() {
        // Not needed for now
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("NeuraFuse", "Accessibility Service Connected");
    }
}
