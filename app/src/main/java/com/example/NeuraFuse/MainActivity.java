// MainActivity.java

package com.example.NeuraFuse;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.app.TimePickerDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import com.example.NeuraFuse.services.MyVpnService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NeuraFuse.adapters.AppUsageAdapter;
import com.example.NeuraFuse.utils.AppLabelHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    private TextView countdownText;
    private Button stopAlarmButton;
    private PieChart pieChart;
    
    private BarChart barChart;
    private RecyclerView recyclerView;
    private UsageStatsManager usageStatsManager;

    private CountDownTimer countDownTimer;
    private MediaPlayer mediaPlayer;
    private Handler alarmHandler = new Handler();
    private Runnable stopAlarmRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please allow Usage Access", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }

        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Enable Accessibility for NeuraFuse", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        Button btnToggleTesting = findViewById(R.id.btnToggleTesting);
        SharedPreferences testPrefs = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE);
        boolean testingMode = testPrefs.getBoolean("testingMode", false);

        btnToggleTesting.setText(testingMode ? "🧪 Testing Mode: ON" : "🧪 Testing Mode: OFF");

        btnToggleTesting.setOnClickListener(v -> {
            boolean isTesting = testPrefs.getBoolean("testingMode", false);
            testPrefs.edit().putBoolean("testingMode", !isTesting).apply();
            btnToggleTesting.setText(!isTesting ? "🧪 Testing Mode: ON" : "🧪 Testing Mode: OFF");
            Toast.makeText(this, "Testing Mode " + (!isTesting ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });

        countdownText = findViewById(R.id.focusCountdown);
        stopAlarmButton = findViewById(R.id.btnStopAlarm);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        recyclerView = findViewById(R.id.recyclerAllApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

        stopAlarmButton.setVisibility(View.GONE);
        stopAlarmButton.setOnClickListener(v -> stopAlarm());

        findViewById(R.id.btnSelectPrimary).setOnClickListener(v ->
                com.example.NeuraFuse.utils.AppListDialog.showPrimaryAppSelector(MainActivity.this));

        findViewById(R.id.btnSchedule).setOnClickListener(v -> pickFocusTime());

        loadUsageStats();
    }

    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private void pickFocusTime() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog startPicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            int startHour = hourOfDay;
            int startMinute = minute;

            SharedPreferences prefs = getSharedPreferences("FocusSchedule", MODE_PRIVATE);
            prefs.edit().putInt("hour", startHour).putInt("minute", startMinute).apply();

            TimePickerDialog endPicker = new TimePickerDialog(this, (view1, endHour, endMinute) ->
                    scheduleFocusMode(startHour, startMinute, endHour, endMinute),
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);

            endPicker.setTitle("Select End Time");
            endPicker.show();
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);

        startPicker.setTitle("Select Start Time");
        startPicker.show();
    }

    private void scheduleFocusMode(int startHour, int startMinute, int endHour, int endMinute) {
        long now = System.currentTimeMillis();

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, startHour);
        start.set(Calendar.MINUTE, startMinute);
        start.set(Calendar.SECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, endHour);
        end.set(Calendar.MINUTE, endMinute);
        end.set(Calendar.SECOND, 0);

        long startMillis = start.getTimeInMillis();
        long endMillis = end.getTimeInMillis();

        SharedPreferences prefs = getSharedPreferences("FocusPrefs", MODE_PRIVATE);
        boolean vpnGranted = prefs.getBoolean("vpnGranted", false);

        if (!vpnGranted) {
            Intent vpnIntent = VpnService.prepare(this);
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, 1001);
                return;
            } else {
                prefs.edit().putBoolean("vpnGranted", true).apply();
            }
        }

        if (now >= endMillis) {
            Toast.makeText(this, "End time is in the past!", Toast.LENGTH_SHORT).show();
            return;
        }

        Handler handler = new Handler();

        if (now < startMillis) {
            long startDelay = startMillis - now;
            long focusDuration = endMillis - startMillis;

            handler.postDelayed(() -> {
                SharedPreferences prefsFlag = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE);
                prefsFlag.edit().putBoolean("isFocusModeOn", true).apply();

                startFocusMode(focusDuration);
            }, startDelay);

            Toast.makeText(this, "Focus Mode Scheduled", Toast.LENGTH_SHORT).show();
        } else {
            long focusDuration = endMillis - now;

            SharedPreferences prefsFlag = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE);
            prefsFlag.edit().putBoolean("isFocusModeOn", true).apply();

            startFocusMode(focusDuration);
            Toast.makeText(this, "Focus Mode Started", Toast.LENGTH_SHORT).show();
        }

        long endDelay = endMillis - now;
        handler.postDelayed(() -> {
            stopService(new Intent(this, com.example.NeuraFuse.services.ForegroundMonitorService.class));
            stopService(new Intent(this, com.example.NeuraFuse.services.MyVpnService.class));

            SharedPreferences prefsFlag = getSharedPreferences("NeuraFusePrefs", MODE_PRIVATE);
            prefsFlag.edit().putBoolean("isFocusModeOn", false).apply();

            Toast.makeText(this, "Focus Mode Ended", Toast.LENGTH_SHORT).show();
        }, endDelay);
    }

    private void startFocusMode(long focusDuration) {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Please enable Accessibility for NeuraFuse", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }

        startService(new Intent(this, com.example.NeuraFuse.services.ForegroundMonitorService.class));
        startService(new Intent(this, com.example.NeuraFuse.services.MyVpnService.class));

        startCountdown(focusDuration);
        Toast.makeText(this, "Focus Mode Started", Toast.LENGTH_SHORT).show();
    }

    private void startCountdown(long millisInFuture) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                countdownText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            public void onFinish() {
                countdownText.setText("00:00");
                playAlarm();
            }
        }.start();
    }

    private void playAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;

        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        stopAlarmButton.setVisibility(View.VISIBLE);

        stopAlarmRunnable = this::stopAlarm;
        alarmHandler.postDelayed(stopAlarmRunnable, 15000);
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        stopAlarmButton.setVisibility(View.GONE);
        alarmHandler.removeCallbacks(stopAlarmRunnable);
    }

    private void loadUsageStats() {
        long endTime = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long beginTime = cal.getTimeInMillis();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);

        Map<String, Long> usageMap = new HashMap<>();
        for (UsageStats stat : usageStatsList) {
            long time = stat.getTotalTimeInForeground();
            if (time > 0) usageMap.put(stat.getPackageName(), time);
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(usageMap.entrySet());
        Collections.sort(sorted, (a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<Map.Entry<String, Long>> top5 = sorted.subList(0, Math.min(5, sorted.size()));
        showPieChart(top5);
        showBarChart(top5);
        recyclerView.setAdapter(new AppUsageAdapter(this, sorted));
    }

    private void showPieChart(List<Map.Entry<String, Long>> stats) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = getColors();
        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, Long> entry : stats) {
            String name = AppLabelHelper.getAppLabel(this, entry.getKey());
            if (seen.contains(name)) continue;
            seen.add(name);
            float minutes = entry.getValue() / (1000f * 60f);
            entries.add(new PieEntry(minutes, name));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Top 5 Apps");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.setDescription(new Description());
        pieChart.setDrawHoleEnabled(false);
        pieChart.invalidate();
    }

    private void showBarChart(List<Map.Entry<String, Long>> stats) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Integer> colors = getColors();
        Set<String> seen = new HashSet<>();

        int index = 0;
        for (Map.Entry<String, Long> entry : stats) {
            String name = AppLabelHelper.getAppLabel(this, entry.getKey());
            if (seen.contains(name)) continue;
            seen.add(name);
            float minutes = entry.getValue() / (1000f * 60f);
            entries.add(new BarEntry(index++, minutes));
            labels.add(name);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Usage (mins)");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(10f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setText("");
        barChart.invalidate();
    }

    private ArrayList<Integer> getColors() {
        return new ArrayList<>(Arrays.asList(
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#4ECDC4"),
                Color.parseColor("#FFD93D"),
                Color.parseColor("#A29BFE"),
                Color.parseColor("#55EFC4")
        ));
    }

    public boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startVpnService();
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show();
        }
    }


    // ✅ Save selected primary app package names
    public static void savePrimaryApps(Context context, List<String> selectedPackageNames) {
        SharedPreferences prefs = context.getSharedPreferences("NeuraPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> set = new HashSet<>(selectedPackageNames);
        editor.putStringSet("primary_apps", set);
        editor.apply();
    }

    // ✅ Retrieve saved primary app package names
    private Set<String> getPrimaryApps() {
        SharedPreferences prefs = getSharedPreferences("NeuraPrefs", MODE_PRIVATE);
        return prefs.getStringSet("primary_apps", new HashSet<>());
    }
    private void startVpnService() {
        Intent vpnIntent = new Intent(this, MyVpnService.class);
        startService(vpnIntent);
        Toast.makeText(this, "Starting VPN...", Toast.LENGTH_SHORT).show();
    }
}
