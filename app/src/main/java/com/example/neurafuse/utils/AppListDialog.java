package com.example.neurafuse.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

import com.example.neurafuse.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListDialog {

    private static final String PREFS = "NeuraPrefs";
    private static final String KEY_PRIMARY = "primary_apps";

    /**
     * Shows a multi-select dialog for choosing primary apps.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void showPrimaryAppSelector(Context context) {
        try {
            List<AppUsageManager.AppUsage> allApps = AppUsageManager.getAllUsedApps(context);
            if (allApps == null || allApps.isEmpty()) {
                Log.w("AppListDialog", "No usage data found — maybe missing permission?");
                return;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Set<String> saved = prefs.getStringSet(KEY_PRIMARY, new HashSet<>());

            List<String> appNames = new ArrayList<>();
            List<String> packageNames = new ArrayList<>();
            boolean[] checked = new boolean[allApps.size()];

            for (int i = 0; i < allApps.size(); i++) {
                AppUsageManager.AppUsage app = allApps.get(i);
                appNames.add(app.appName);
                packageNames.add(app.packageName);
                checked[i] = saved != null && saved.contains(app.packageName);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Select Primary Apps");

            builder.setMultiChoiceItems(appNames.toArray(new String[0]), checked,
                    (dialog, which, isChecked) -> checked[which] = isChecked);

            builder.setPositiveButton("Save", (dialog, which) -> {
                Set<String> selected = new HashSet<>();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) selected.add(packageNames.get(i));
                }
                prefs.edit().putStringSet(KEY_PRIMARY, selected).apply();
                Log.d("AppListDialog", "Saved primary apps: " + selected);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();

        } catch (Exception e) {
            Log.e("AppListDialog", "Error showing app selector", e);
        }
    }
}
