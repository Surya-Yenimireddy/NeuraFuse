package com.example.neurafuse;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PrimaryAppSelectorActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "NeuraPrefs";
    private static final String KEY_PRIMARY_APPS = "primary_apps";

    private RecyclerView appRecyclerView;
    private EditText searchBar;
    private PackageManager pm;
    private AppListAdapter adapter;
    private List<ApplicationInfo> appList;
    private Set<String> selectedApps = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primary_app_selector);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        appRecyclerView = findViewById(R.id.appRecyclerView);
        searchBar = findViewById(R.id.searchBar);
        pm = getPackageManager();

        loadSelectedApps();
        updateSelectedCount();
        new LoadAppsTask().execute();

        findViewById(R.id.saveButton).setOnClickListener(v -> saveSelectedApps());
    }

    private void updateSelectedCount() {
        TextView countView = findViewById(R.id.selectedCount);
        if (countView != null) {
            countView.setText(selectedApps.size() + " apps selected");
        }
    }

    private void loadSelectedApps() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedApps = prefs.getStringSet(KEY_PRIMARY_APPS, new HashSet<>());
    }

    /** Loads all launcher-visible apps (user + system) */
    private class LoadAppsTask extends AsyncTask<Void, Void, List<ApplicationInfo>> {
        @Override
        protected List<ApplicationInfo> doInBackground(Void... voids) {
            List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            List<ApplicationInfo> visibleApps = allApps.stream()
                    .filter(app -> pm.getLaunchIntentForPackage(app.packageName) != null) // only launcher-visible
                    .filter(app -> !isJunkApp(pm.getApplicationLabel(app).toString().toLowerCase())) // filter system junk
                    .collect(Collectors.toList());

            // Sort alphabetically
            Collections.sort(visibleApps, Comparator.comparing(a -> pm.getApplicationLabel(a).toString().toLowerCase()));

            return visibleApps;
        }

        @Override
        protected void onPostExecute(List<ApplicationInfo> apps) {
            appList = apps;
            adapter = new AppListAdapter(appList, pm, selectedApps);
            appRecyclerView.setLayoutManager(new LinearLayoutManager(PrimaryAppSelectorActivity.this));
            appRecyclerView.setAdapter(adapter);
            setupSearch();
        }
    }

    /** Filters out system junk even if they have launcher icons */
    private boolean isJunkApp(String name) {
        return name.contains("android system")
                || name.contains("webview")
                || name.contains("switch access")
                || name.contains("simple view")
                || name.contains("setup wizard")
                || name.contains("browser plug")
                || name.contains("device help")
                || name.contains("smart switch")
                || name.contains("tracking");
    }

    /** Real-time search filter */
    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                List<ApplicationInfo> filtered = appList.stream()
                        .filter(app -> pm.getApplicationLabel(app).toString().toLowerCase().contains(query))
                        .collect(Collectors.toList());
                adapter = new AppListAdapter(filtered, pm, selectedApps);
                appRecyclerView.setAdapter(adapter);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void saveSelectedApps() {
        // Mark all non-launchable background/system apps as primary automatically
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : allApps) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                selectedApps.add(appInfo.packageName);
            }
        }

        // Always include NeuraFuse itself
        selectedApps.add(getPackageName());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_PRIMARY_APPS, selectedApps).apply();

        Toast.makeText(this, "✅ Primary apps saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
