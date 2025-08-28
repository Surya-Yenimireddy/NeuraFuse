package com.example.NeuraFuse.utils;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppUsageManager {

    public static class AppUsage {
        public String appName;
        public String packageName;
        public long usageTimeMillis;
        public Drawable icon;

        public AppUsage(String appName, String packageName, long usageTimeMillis, Drawable icon) {
            this.appName = appName;
            this.packageName = packageName;
            this.usageTimeMillis = usageTimeMillis;
            this.icon = icon;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static List<AppUsage> getTopUsedApps(Context context, int maxApps) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -1); // Last 24 hours
        long startTime = calendar.getTimeInMillis();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        PackageManager packageManager = context.getPackageManager();
        List<AppUsage> appUsageList = new ArrayList<>();

        for (UsageStats usageStats : usageStatsList) {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(usageStats.getPackageName(), 0);
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue; // Skip system apps

                String appName = packageManager.getApplicationLabel(appInfo).toString();
                Drawable icon = packageManager.getApplicationIcon(appInfo);
                long usageTime = usageStats.getTotalTimeInForeground();

                if (usageTime > 0) {
                    appUsageList.add(new AppUsage(appName, usageStats.getPackageName(), usageTime, icon));
                }

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Sort by usage time descending
        Collections.sort(appUsageList, new Comparator<AppUsage>() {
            @Override
            public int compare(AppUsage a1, AppUsage a2) {
                return Long.compare(a2.usageTimeMillis, a1.usageTimeMillis);
            }
        });

        // Return top N apps
        return appUsageList.subList(0, Math.min(maxApps, appUsageList.size()));
    }

    public static List<AppUsage> getAllUsedApps(Context context) {
        return getTopUsedApps(context, Integer.MAX_VALUE);
    }
}