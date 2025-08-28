package com.example.NeuraFuse.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

public class AppClassifier {

    public enum AppCategory {
        PRODUCTIVE,
        SOCIAL,
        ENTERTAINMENT,
        UTILITY,
        UNKNOWN
    }

    private static final String[] PRODUCTIVE_KEYWORDS = {
            "zoom", "meet", "classroom", "docs", "mail", "teams", "skype"
    };

    private static final String[] SOCIAL_KEYWORDS = {
            "whatsapp", "facebook", "instagram", "snapchat", "telegram", "messenger"
    };

    private static final String[] ENTERTAINMENT_KEYWORDS = {
            "youtube", "netflix", "hotstar", "primevideo", "tiktok", "mxplayer"
    };

    private static final String[] UTILITY_KEYWORDS = {
            "clock", "calculator", "settings", "gallery", "file", "camera"
    };

    private final PackageManager packageManager;
    private final Map<String, AppCategory> appCategoryMap = new HashMap<>();

    public AppClassifier(Context context) {
        this.packageManager = context.getPackageManager();
        loadInstalledApps(context);
    }

    private void loadInstalledApps(Context context) {
        for (ApplicationInfo appInfo : packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
            String packageName = appInfo.packageName.toLowerCase();
            AppCategory category = classifyApp(packageName);
            appCategoryMap.put(packageName, category);
        }
    }

    public AppCategory getCategory(String packageName) {
        return appCategoryMap.getOrDefault(packageName.toLowerCase(), AppCategory.UNKNOWN);
    }

    private AppCategory classifyApp(String packageName) {
        for (String keyword : PRODUCTIVE_KEYWORDS)
            if (packageName.contains(keyword)) return AppCategory.PRODUCTIVE;

        for (String keyword : SOCIAL_KEYWORDS)
            if (packageName.contains(keyword)) return AppCategory.SOCIAL;

        for (String keyword : ENTERTAINMENT_KEYWORDS)
            if (packageName.contains(keyword)) return AppCategory.ENTERTAINMENT;

        for (String keyword : UTILITY_KEYWORDS)
            if (packageName.contains(keyword)) return AppCategory.UTILITY;

        return AppCategory.UNKNOWN;
    }
}