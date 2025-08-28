package com.example.NeuraFuse.services;

import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ForegroundMonitorService extends Service {

    private Handler handler;
    private Runnable monitorRunnable;
    private UsageStatsManager usageStatsManager;
    private Set<String> primaryApps = new HashSet<>();  // ✅ FIXED: Declare here
    private static final String TAG = "NeuraFuseMonitor";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        loadPrimaryApps();  // ✅ Load primary app list
        startMonitoring();
    }

    private void loadPrimaryApps() {
        SharedPreferences prefs = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE);
        primaryApps = prefs.getStringSet("primaryApps", new HashSet<>());
        Log.d(TAG, "Loaded primary apps: " + primaryApps);
    }

    private void startMonitoring() {
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE);
                boolean isFocusModeOn = prefs.getBoolean("isFocusModeOn", false);
                boolean isTesting = prefs.getBoolean("testingMode", false);

                long remaining = prefs.getLong("remainingTime", 0);
                if (!isFocusModeOn || isTesting || remaining <= 0) {
                    handler.postDelayed(this, 1000);
                    return;
                }


                String topApp = getTopAppPackage();

                if (topApp != null && !primaryApps.contains(topApp)) {
                    Log.d(TAG, "Non-primary app detected: " + topApp + " — sending to background");

                    // Push app to background
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                }

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(monitorRunnable);
    }

    private String getTopAppPackage() {
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - 3000;

        UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastApp = null;

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastApp = event.getPackageName();
            }
        }

        return lastApp;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(monitorRunnable);
        SharedPreferences.Editor editor = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE).edit();
        editor.putBoolean("isFocusModeOn", false);
        editor.putLong("remainingTime", 0);
        editor.apply();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
