package com.example.neurafuse.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.neurafuse.MainActivity;
import com.example.neurafuse.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Improved ForegroundMonitorService
 * - Only runs enforcement while KEY_RUNNING = true in SharedPreferences
 * - Counts only REAL foreground switches (prevents duplicate event spam)
 * - Uses exactly MAX_CHANCES warnings before pushing to HOME
 * - Resets counters on app switch or when primary app active
 */
public class ForegroundMonitorService extends AccessibilityService {

    private static final String TAG = "NeuraFuseMonitor";
    public static final String ACTION_START_FOREGROUND = "com.example.neurafuse.action.START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "com.example.neurafuse.action.STOP_FOREGROUND";
    private static final String PREFS = "NeuraPrefs";
    private static final String KEY_PRIMARY_APPS = "primary_apps";
    private static final String KEY_RUNNING = "isFocusRunning";

    private static final int MAX_CHANCES = 3;
    private static final long DEBOUNCE_MS = 800L; // short debounce

    private SharedPreferences prefs;
    private PackageManager pm;
    private Set<String> primaryApps = new HashSet<>();

    // warnings map and last foreground tracking
    private final Map<String, Integer> appWarnings = new HashMap<>();
    private String lastForegroundPkg = null;
    private final Map<String, Long> lastEventTime = new HashMap<>();

    private static final String CHANNEL_ID = "NeuraFuseMonitorChannel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        pm = getPackageManager();
        loadPrimaryApps();
        createNotificationChannel();
        startPersistentNotification();
        Log.i(TAG, "ForegroundMonitorService created. primaryApps=" + primaryApps);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 50;
        setServiceInfo(info);
        Log.i(TAG, "Accessibility service connected.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // handle explicit start/stop actions
        if (intent != null && intent.getAction() != null) {
            if (ACTION_START_FOREGROUND.equals(intent.getAction())) {
                startPersistentNotification();
                // reset state when starting focus
                appWarnings.clear();
                lastForegroundPkg = null;
                lastEventTime.clear();
            } else if (ACTION_STOP_FOREGROUND.equals(intent.getAction())) {
                // stop focus: clear flag and stop
                prefs.edit().putBoolean(KEY_RUNNING, false).apply();
                try {
                    stopForeground(true);
                    stopSelf();
                } catch (Exception ignored) {}
            }
        }
        return START_STICKY;
    }

    private void loadPrimaryApps() {
        primaryApps.clear();
        Set<String> saved = prefs.getStringSet(KEY_PRIMARY_APPS, new HashSet<>());
        if (saved == null || saved.isEmpty()) {
            primaryApps.add(getPackageName());
            primaryApps.add("com.android.settings");
            primaryApps.add("com.android.systemui");
            return;
        }

        // try to resolve labels to package names
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (String entry : saved) {
            if (entry == null) continue;
            String s = entry.trim();
            if (s.isEmpty()) continue;
            if (s.contains(".")) {
                primaryApps.add(s);
                continue;
            }
            boolean found = false;
            for (ApplicationInfo ai : installed) {
                CharSequence label = pm.getApplicationLabel(ai);
                if (label != null && label.toString().equalsIgnoreCase(s)) {
                    primaryApps.add(ai.packageName);
                    found = true;
                    break;
                }
            }
            if (!found) primaryApps.add(s);
        }
        primaryApps.add(getPackageName());
        primaryApps.add("com.android.settings");
        primaryApps.add("com.android.systemui");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "NeuraFuse Focus Mode", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("NeuraFuse background monitoring");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void startPersistentNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("NeuraFuse – Focus Mode Active")
                .setContentText("Monitoring distractions...")
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event == null || event.getPackageName() == null) return;

            // Only enforce when focus mode is enabled
            boolean isFocusOn = prefs.getBoolean(KEY_RUNNING, false);
            if (!isFocusOn) return;

            String pkg = event.getPackageName().toString();
            long now = SystemClock.elapsedRealtime();

            // Ignore quick duplicate events for same package
            long lastTime = lastEventTime.getOrDefault(pkg, 0L);
            if (now - lastTime < DEBOUNCE_MS) {
                lastEventTime.put(pkg, now);
                return;
            }
            lastEventTime.put(pkg, now);

            // Determine real foreground app — use usage events if needed
            String currentForeground = getForegroundApp();
            if (currentForeground == null) {
                // fallback to event package
                currentForeground = pkg;
            }

            // If foreground hasn't changed, do nothing
            if (currentForeground.equals(lastForegroundPkg)) {
                return;
            }

            // Foreground changed: reset other apps' counters (only maintain current app warnings)
            for (String k : new HashSet<>(appWarnings.keySet())) {
                if (!k.equals(currentForeground)) appWarnings.put(k, 0);
            }

            lastForegroundPkg = currentForeground;

            // Ignore system / our app
            if (currentForeground.equals(getPackageName()) || currentForeground.startsWith("com.android.systemui")) {
                return;
            }

            // If primary, clear warnings and return
            if (primaryApps.contains(currentForeground)) {
                appWarnings.remove(currentForeground);
                updateNotification("Primary: " + getAppName(currentForeground));
                return;
            }

            // This is a non-primary app that just came to foreground -> increment its warnings
            int count = appWarnings.getOrDefault(currentForeground, 0) + 1;
            appWarnings.put(currentForeground, count);
            String appName = getAppName(currentForeground);
            Log.d(TAG, "Foreground detected: " + currentForeground + " count=" + count);

            if (count < MAX_CHANCES) {
                String msg = "⚠ " + appName + " (" + count + "/" + MAX_CHANCES + ")";
                showShortToast(msg);
                updateNotification(msg);
                try {
                    MyVpnService.blockApp(currentForeground);
                } catch (Exception e) {
                    Log.w(TAG, "VPN block (warn) failed: " + e.getMessage());
                }
            } else {
                // count >= MAX_CHANCES -> lock/push to background
                String msg = "🚫 Focus Lock: " + appName;
                showShortToast(msg);
                updateNotification(msg);
                // Push to home
                boolean res = performGlobalAction(GLOBAL_ACTION_HOME);
                Log.i(TAG, "performGlobalAction(HOME) result=" + res + " for " + currentForeground);
                // final block via VPN
                try {
                    MyVpnService.blockApp(currentForeground);
                } catch (Exception e) {
                    Log.w(TAG, "VPN block (final) failed: " + e.getMessage());
                }
                // keep warnings at MAX to avoid immediate re-trigger
                appWarnings.put(currentForeground, MAX_CHANCES);
            }

        } catch (Exception ex) {
            Log.e(TAG, "onAccessibilityEvent error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Attempts to determine the foreground package using UsageEvents.
     */
    private String getForegroundApp() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return null;
            long end = System.currentTimeMillis();
            long begin = end - 3000;
            UsageEvents events = usm.queryEvents(begin, end);
            UsageEvents.Event ev = new UsageEvents.Event();
            String last = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(ev);
                if (ev.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    last = ev.getPackageName();
                }
            }
            return last;
        } catch (Exception e) {
            Log.w(TAG, "getForegroundApp failed: " + e.getMessage());
            return null;
        }
    }

    private boolean isUserVisibleApp(String pkg) {
        try {
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            return launch != null && !pkg.equals(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    private void updateNotification(String msg) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("NeuraFuse – Focus Mode")
                .setContentText(msg)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        if (nm != null) nm.notify(NOTIFICATION_ID, n);
    }

    private String getAppName(String pkg) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private void showShortToast(String t) {
        new Handler(getMainLooper()).post(() -> Toast.makeText(this, t, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility interrupted");
    }
}
