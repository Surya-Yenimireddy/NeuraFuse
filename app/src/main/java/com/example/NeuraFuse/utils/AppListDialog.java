package com.example.NeuraFuse.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.NeuraFuse.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListDialog {

    public static void showPrimaryAppSelector(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppItem> appItems = new ArrayList<>();

        for (ApplicationInfo app : installedApps) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                String label = pm.getApplicationLabel(app).toString();
                Drawable icon = pm.getApplicationIcon(app);
                appItems.add(new AppItem(label, app.packageName, icon));
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("NeuraFusePrefs", Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet("primaryApps", new HashSet<>());
        Set<String> selectedPackages = new HashSet<>(saved);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Primary Apps");

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        for (AppItem item : appItems) {
            View row = LayoutInflater.from(context).inflate(R.layout.app_list_item, null);
            TextView label = row.findViewById(R.id.appLabel);
            ImageView icon = row.findViewById(R.id.appIcon);
            View checkbox = row.findViewById(R.id.checkbox);

            label.setText(item.label);
            icon.setImageDrawable(item.icon);

            checkbox.setBackgroundResource(
                    selectedPackages.contains(item.packageName)
                            ? R.drawable.checkbox_checked
                            : R.drawable.checkbox_unchecked
            );

            row.setOnClickListener(v -> {
                if (selectedPackages.contains(item.packageName)) {
                    selectedPackages.remove(item.packageName);
                    checkbox.setBackgroundResource(R.drawable.checkbox_unchecked);
                } else {
                    selectedPackages.add(item.packageName);
                    checkbox.setBackgroundResource(R.drawable.checkbox_checked);
                }
            });

            layout.addView(row);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("OK", (dialog, which) -> {
            prefs.edit().putStringSet("primaryApps", selectedPackages).apply();
            Toast.makeText(context, "✅ Primary apps saved", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public static class AppItem {
        public String label;
        public String packageName;
        public Drawable icon;

        public AppItem(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }
}
