package com.example.neurafuse;

import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.*;

public class DigitalWellbeingActivity extends AppCompatActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout mostUsedAppsList, categoryList;
    private TextView totalScreenTime, comparisonText;
    private PackageManager pm;
    private UsageStatsManager usm;

    // App categories
    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();
    static {
        // Social Media
        CATEGORY_MAP.put("com.instagram.android", "Social Media");
        CATEGORY_MAP.put("com.twitter.android", "Social Media");
        CATEGORY_MAP.put("com.facebook.katana", "Social Media");
        CATEGORY_MAP.put("com.snapchat.android", "Social Media");
        CATEGORY_MAP.put("com.reddit.frontpage", "Social Media");
        CATEGORY_MAP.put("com.linkedin.android", "Social Media");
        CATEGORY_MAP.put("com.pinterest", "Social Media");
        // Entertainment
        CATEGORY_MAP.put("com.google.android.youtube", "Entertainment");
        CATEGORY_MAP.put("com.netflix.mediaclient", "Entertainment");
        CATEGORY_MAP.put("com.spotify.music", "Entertainment");
        CATEGORY_MAP.put("in.startv.hotstar", "Entertainment");
        CATEGORY_MAP.put("com.amazon.avod.thirdpartyclient", "Entertainment");
        // Communication
        CATEGORY_MAP.put("com.whatsapp", "Communication");
        CATEGORY_MAP.put("org.telegram.messenger", "Communication");
        CATEGORY_MAP.put("com.discord", "Communication");
        CATEGORY_MAP.put("com.google.android.gm", "Communication");
        // Productivity
        CATEGORY_MAP.put("com.google.android.apps.docs", "Productivity");
        CATEGORY_MAP.put("com.microsoft.office.word", "Productivity");
        CATEGORY_MAP.put("com.notion.id", "Productivity");
        // Education
        CATEGORY_MAP.put("com.duolingo", "Education");
        CATEGORY_MAP.put("com.udemy.android", "Education");
    }

    private static final Map<String, Integer> CATEGORY_COLORS = new HashMap<>();
    static {
        CATEGORY_COLORS.put("Social Media", Color.parseColor("#FF5C5C"));
        CATEGORY_COLORS.put("Entertainment", Color.parseColor("#FFB547"));
        CATEGORY_COLORS.put("Productivity", Color.parseColor("#00E5A0"));
        CATEGORY_COLORS.put("Education", Color.parseColor("#4F6EF7"));
        CATEGORY_COLORS.put("Communication", Color.parseColor("#06B6D4"));
        CATEGORY_COLORS.put("Other", Color.parseColor("#A855F7"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_wellbeing);

        pieChart = findViewById(R.id.pieChartToday);
        barChart = findViewById(R.id.barChartWeek);
        mostUsedAppsList = findViewById(R.id.mostUsedAppsList);
        categoryList = findViewById(R.id.categoryList);
        totalScreenTime = findViewById(R.id.totalScreenTime);
        comparisonText = findViewById(R.id.comparisonText);
        pm = getPackageManager();
        usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        showTodayUsage();
        showCategoryBreakdown();
        showWeeklyUsage();
        showMostUsedApps();
    }

    private void showTodayUsage() {
        long startOfDay = getStartOfDayMillis();
        long now = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);
        Map<String, Long> appUsage = new HashMap<>();
        long grandTotal = 0;

        for (UsageStats usage : stats) {
            long time = usage.getTotalTimeInForeground();
            if (time > 0 && !isSystemApp(usage.getPackageName())) {
                String appName = getAppName(usage.getPackageName());
                appUsage.put(appName, appUsage.getOrDefault(appName, 0L) + time);
                grandTotal += time;
            }
        }

        appUsage.replaceAll((k, v) -> Math.min(v, 86400000L));
        appUsage.entrySet().removeIf(e -> e.getValue() < 60000);

        // Set total screen time
        totalScreenTime.setText(formatTime(grandTotal));

        // Comparison with yesterday (approximate)
        long yesterdayStart = startOfDay - 86400000L;
        List<UsageStats> yesterdayStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, yesterdayStart, startOfDay);
        long yesterdayTotal = 0;
        for (UsageStats u : yesterdayStats) {
            if (!isSystemApp(u.getPackageName())) yesterdayTotal += u.getTotalTimeInForeground();
        }
        if (yesterdayTotal > 0) {
            long diff = grandTotal - yesterdayTotal;
            int percent = (int) ((Math.abs(diff) * 100) / yesterdayTotal);
            if (diff < 0) {
                comparisonText.setText("↓ " + percent + "% less than yesterday ✅");
            } else {
                comparisonText.setText("↑ " + percent + "% more than yesterday");
            }
        } else {
            comparisonText.setText("No data from yesterday");
        }

        // Pie chart
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(appUsage.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        if (sorted.size() > 6) sorted = sorted.subList(0, 6);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sorted) {
            float hours = entry.getValue() / 3600000f;
            entries.add(new PieEntry(hours, entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#4F6EF7"), Color.parseColor("#00E5A0"),
                Color.parseColor("#FF5C5C"), Color.parseColor("#FFB547"),
                Color.parseColor("#A855F7"), Color.parseColor("#06B6D4")
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(11f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLineColor(Color.parseColor("#6B6F8D"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hr = (int) value;
                int min = Math.round((value - hr) * 60);
                if (hr > 0 && min > 0) return hr + "h " + min + "m";
                else if (hr > 0) return hr + "h";
                else return min + "m";
            }
        });

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setCenterText("Today\n" + formatTime(grandTotal));
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setEntryLabelColor(Color.parseColor("#A0A3BD"));
        pieChart.setEntryLabelTextSize(10f);
        pieChart.setHoleColor(Color.parseColor("#1A1D35"));
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.setTransparentCircleColor(Color.parseColor("#1A1D35"));
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(10, 10, 10, 20);
        pieChart.setMinAngleForSlices(10f);
        pieChart.animateY(1200);
        pieChart.invalidate();
    }

    private void showCategoryBreakdown() {
        long startOfDay = getStartOfDayMillis();
        long now = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);

        Map<String, Long> catTotals = new LinkedHashMap<>();
        catTotals.put("Social Media", 0L);
        catTotals.put("Entertainment", 0L);
        catTotals.put("Communication", 0L);
        catTotals.put("Productivity", 0L);
        catTotals.put("Education", 0L);
        catTotals.put("Other", 0L);

        for (UsageStats usage : stats) {
            long time = usage.getTotalTimeInForeground();
            if (time > 60000 && !isSystemApp(usage.getPackageName())) {
                String cat = CATEGORY_MAP.getOrDefault(usage.getPackageName(), "Other");
                catTotals.put(cat, catTotals.getOrDefault(cat, 0L) + time);
            }
        }

        categoryList.removeAllViews();
        for (Map.Entry<String, Long> entry : catTotals.entrySet()) {
            if (entry.getValue() < 60000) continue;
            categoryList.addView(createCategoryCard(entry.getKey(), entry.getValue()));
        }
    }

    private View createCategoryCard(String category, long millis) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.parseColor("#1A1D35"));
        card.setRadius(dpToPx(12));
        card.setCardElevation(0);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Color dot
        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(12), dpToPx(12));
        dotParams.setMargins(0, 0, dpToPx(12), 0);
        dot.setLayoutParams(dotParams);
        int color = CATEGORY_COLORS.getOrDefault(category, Color.parseColor("#A855F7"));
        dot.setBackgroundColor(color);
        row.addView(dot);

        // Category name
        TextView nameView = new TextView(this);
        nameView.setText(category);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(14);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);

        // Time
        TextView timeView = new TextView(this);
        timeView.setText(formatTime(millis));
        timeView.setTextColor(Color.parseColor("#A0A3BD"));
        timeView.setTextSize(14);
        row.addView(timeView);

        card.addView(row);
        return card;
    }

    private void showWeeklyUsage() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> days = new ArrayList<>();

        cal.add(Calendar.DAY_OF_MONTH, -6);
        for (int i = 0; i < 7; i++) {
            long start = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long end = cal.getTimeInMillis();

            List<UsageStats> dailyStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
            long total = 0;
            for (UsageStats u : dailyStats) {
                if (!isSystemApp(u.getPackageName())) total += u.getTotalTimeInForeground();
            }
            total = Math.min(total, 86400000L);
            entries.add(new BarEntry(i, total / 3600000f));
            days.add(new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(start)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#4F6EF7"));
        dataSet.setValueTextColor(Color.parseColor("#A0A3BD"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry e) {
                float hrs = e.getY();
                int h = (int) hrs;
                int m = Math.round((hrs - h) * 60);
                if (h > 0 && m > 0) return h + "h " + m + "m";
                else if (h > 0) return h + "h";
                else return m + "m";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.parseColor("#A0A3BD"));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setTextColor(Color.parseColor("#6B6F8D"));
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void showMostUsedApps() {
        long startOfDay = getStartOfDayMillis();
        long now = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);

        Map<String, Long> appUsage = new HashMap<>();
        for (UsageStats usage : stats) {
            long time = usage.getTotalTimeInForeground();
            if (time > 60000 && !isSystemApp(usage.getPackageName())) {
                appUsage.put(usage.getPackageName(), appUsage.getOrDefault(usage.getPackageName(), 0L) + time);
            }
        }
        appUsage.replaceAll((k, v) -> Math.min(v, 86400000L));
        appUsage.entrySet().removeIf(e -> e.getValue() < 60000);

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(appUsage.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        mostUsedAppsList.removeAllViews();
        for (Map.Entry<String, Long> entry : sorted) {
            String pkg = entry.getKey();
            String name = getAppName(pkg);
            Drawable icon = getAppIcon(pkg);
            String readable = formatTime(entry.getValue());
            String cat = CATEGORY_MAP.getOrDefault(pkg, "Other");

            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(6));
            card.setLayoutParams(cardParams);
            card.setCardBackgroundColor(Color.parseColor("#1A1D35"));
            card.setRadius(dpToPx(12));
            card.setCardElevation(0);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
            item.setGravity(Gravity.CENTER_VERTICAL);

            ImageView iconView = new ImageView(this);
            iconView.setImageDrawable(icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            iconParams.setMargins(0, 0, dpToPx(12), 0);
            iconView.setLayoutParams(iconParams);
            item.addView(iconView);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            info.setLayoutParams(infoParams);

            TextView nameView = new TextView(this);
            nameView.setText(name);
            nameView.setTextSize(14f);
            nameView.setTextColor(Color.WHITE);
            info.addView(nameView);

            TextView catView = new TextView(this);
            catView.setText(cat);
            catView.setTextSize(11f);
            catView.setTextColor(CATEGORY_COLORS.getOrDefault(cat, Color.parseColor("#A855F7")));
            info.addView(catView);

            item.addView(info);

            TextView timeView = new TextView(this);
            timeView.setText(readable);
            timeView.setTextSize(14f);
            timeView.setTextColor(Color.parseColor("#A0A3BD"));
            item.addView(timeView);

            card.addView(item);
            mostUsedAppsList.addView(card);
        }
    }

    // Utility methods
    private boolean isSystemApp(String pkg) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                    || pkg.startsWith("com.android.") || pkg.startsWith("com.google.") || pkg.startsWith("android");
        } catch (Exception e) { return true; }
    }

    private long getStartOfDayMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String getAppName(String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) { return packageName; }
    }

    private Drawable getAppIcon(String packageName) {
        try {
            return pm.getApplicationIcon(pm.getApplicationInfo(packageName, 0));
        } catch (Exception e) { return getDrawable(android.R.drawable.sym_def_app_icon); }
    }

    private String formatTime(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0) return hours + "h " + minutes + "m";
        else if (hours > 0) return hours + "h";
        else return minutes + "m";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
