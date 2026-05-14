package com.example.neurafuse.utils;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that fetches and aggregates app usage data.
 * Returns only non-system apps used more than one minute.
 */
public class AppUsageManager {

    private static final String TAG = "AppUsageManager";
    private static final long MIN_USAGE_MILLIS = 60 * 1000; // 1 minute

    // Nested model class representing an app and its usage
    public static class AppUsage {
        public final String appName;
        public final String packageName;
        public final long usageTimeMillis;
        public final Drawable icon;

        public AppUsage(String appName, String packageName, long usageTimeMillis, Drawable icon) {
            this.appName = appName;
            this.packageName = packageName;
            this.usageTimeMillis = usageTimeMillis;
            this.icon = icon;
        }

        public String getFormattedTime() {
            long totalMinutes = usageTimeMillis / 1000 / 60;
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (hours > 0) return hours + "h " + minutes + "m";
            else return minutes + "m";
        }
    }

    // Get all used apps for the past 7 days
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static List<AppUsage> getAllUsedApps(Context context) {
        List<AppUsage> appUsageList = new ArrayList<>();

        try {
            UsageStatsManager usageStatsManager =
                    (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usageStatsManager == null) return appUsageList;

            Calendar cal = Calendar.getInstance();
            long endTime = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_YEAR, -7); // last 7 days
            long startTime = cal.getTimeInMillis();

            List<UsageStats> usageStatsList =
                    usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

            if (usageStatsList == null || usageStatsList.isEmpty()) {
                Log.w(TAG, "No usage stats available (missing permission or no data).");
                return appUsageList;
            }

            PackageManager pm = context.getPackageManager();
            Map<String, Long> totalUsage = new HashMap<>();

            for (UsageStats usage : usageStatsList) {
                String pkg = usage.getPackageName();
                long time = usage.getTotalTimeInForeground();
                if (time <= 0) continue;
                totalUsage.put(pkg, totalUsage.getOrDefault(pkg, 0L) + time);
            }

            for (Map.Entry<String, Long> entry : totalUsage.entrySet()) {
                long totalTime = entry.getValue();
                if (totalTime < MIN_USAGE_MILLIS) continue;

                try {
                    ApplicationInfo info = pm.getApplicationInfo(entry.getKey(), 0);
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue; // skip system apps
                    String name = pm.getApplicationLabel(info).toString();
                    Drawable icon = pm.getApplicationIcon(info);
                    appUsageList.add(new AppUsage(name, entry.getKey(), totalTime, icon));
                } catch (PackageManager.NameNotFoundException ignored) { }
            }

            Collections.sort(appUsageList, (a, b) ->
                    Long.compare(b.usageTimeMillis, a.usageTimeMillis));

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving usage stats", e);
        }

        return appUsageList;
    }
}
