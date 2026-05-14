package com.example.neurafuse;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.neurafuse.data.FocusSession;
import com.example.neurafuse.data.FocusSessionDao;
import com.example.neurafuse.data.NeuraFuseDatabase;
import com.example.neurafuse.services.ForegroundMonitorService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int USAGE_ACCESS_REQUEST = 101;
    private static final int VPN_REQUEST = 102;
    private static final long DAILY_GOAL_MILLIS = 4 * 3600000L; // 4 hours

    private TextView greetingText, taglineText;
    private TextView streakCount, streakLabel;
    private TextView todayFocusTime, todaySessions, dailyGoalText;
    private ProgressBar dailyProgress;
    private LinearLayout recentSessionsList;
    private TextView emptySessionText;
    private BottomNavigationView bottomNav;
    private FocusSessionDao sessionDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionDao = NeuraFuseDatabase.getInstance(this).focusSessionDao();

        // Bind views
        greetingText = findViewById(R.id.greetingText);
        taglineText = findViewById(R.id.taglineText);
        streakCount = findViewById(R.id.streakCount);
        streakLabel = findViewById(R.id.streakLabel);
        todayFocusTime = findViewById(R.id.todayFocusTime);
        todaySessions = findViewById(R.id.todaySessions);
        dailyProgress = findViewById(R.id.dailyProgress);
        dailyGoalText = findViewById(R.id.dailyGoalText);
        recentSessionsList = findViewById(R.id.recentSessionsList);
        emptySessionText = findViewById(R.id.emptySessionText);
        bottomNav = findViewById(R.id.bottomNav);

        setupGreeting();
        setupBottomNav();
        setupQuickPresets();
        requestAllPermissions();

        findViewById(R.id.btnViewAll).setOnClickListener(v ->
            startActivity(new Intent(this, SessionHistoryActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_home);
        loadDashboardData();
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            greetingText.setText("Good Morning 👋");
            taglineText.setText("Let's start the day focused");
        } else if (hour < 17) {
            greetingText.setText("Good Afternoon 👋");
            taglineText.setText("Stay productive this afternoon");
        } else {
            greetingText.setText("Good Evening 👋");
            taglineText.setText("Wrap up with a focus session");
        }
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_focus) {
                startActivity(new Intent(this, FocusModeActivity.class));
                return true;
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, DigitalWellbeingActivity.class));
                return true;
            } else if (id == R.id.nav_apps) {
                startActivity(new Intent(this, PrimaryAppSelectorActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupQuickPresets() {
        findViewById(R.id.presetPomodoro).setOnClickListener(v ->
                launchFocusWithPreset(25, "pomodoro"));
        findViewById(R.id.presetDeepWork).setOnClickListener(v ->
                launchFocusWithPreset(60, "deep_work"));
        findViewById(R.id.presetStudy).setOnClickListener(v ->
                launchFocusWithPreset(120, "study"));
    }

    private void launchFocusWithPreset(int minutes, String type) {
        Intent intent = new Intent(this, FocusModeActivity.class);
        intent.putExtra("preset_minutes", minutes);
        intent.putExtra("session_type", type);
        startActivity(intent);
    }

    private void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long startOfDay = getStartOfDayMillis();

            long focusToday = sessionDao.getTotalFocusTimeToday(startOfDay);
            int sessionsToday = sessionDao.getCompletedSessionsToday(startOfDay);
            int streak = calculateStreak();
            List<FocusSession> recent = sessionDao.getRecentSessions();

            runOnUiThread(() -> {
                // Today's focus
                todayFocusTime.setText(formatDuration(focusToday));
                todaySessions.setText(sessionsToday + " session" + (sessionsToday != 1 ? "s" : ""));

                // Progress
                int progress = (int) ((focusToday * 100) / DAILY_GOAL_MILLIS);
                dailyProgress.setProgress(Math.min(progress, 100));
                dailyGoalText.setText("Goal: 4 hours daily • " + progress + "% done");

                // Streak
                streakCount.setText(String.valueOf(streak));
                streakLabel.setText(streak == 1 ? "day streak 🔥" : "day streak 🔥");

                // Recent sessions
                recentSessionsList.removeAllViews();
                if (recent.isEmpty()) {
                    emptySessionText.setVisibility(View.VISIBLE);
                } else {
                    emptySessionText.setVisibility(View.GONE);
                    for (FocusSession s : recent) {
                        recentSessionsList.addView(createSessionCard(s));
                    }
                }
            });
        });
    }

    private View createSessionCard(FocusSession session) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getColor(R.color.bg_card));
        card.setRadius(dpToPx(12));
        card.setCardElevation(0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        content.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Status dot
        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(10));
        dotParams.setMargins(0, 0, dpToPx(12), 0);
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(session.completed ? R.color.success : R.color.danger);
        content.addView(dot);

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        info.setLayoutParams(infoParams);

        TextView typeText = new TextView(this);
        typeText.setText(formatSessionType(session.sessionType));
        typeText.setTextColor(getColor(R.color.text_primary));
        typeText.setTextSize(14);
        info.addView(typeText);

        TextView timeText = new TextView(this);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(session.startTime);
        timeText.setText(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        timeText.setTextColor(getColor(R.color.text_hint));
        timeText.setTextSize(12);
        info.addView(timeText);

        content.addView(info);

        // Duration
        TextView durText = new TextView(this);
        durText.setText(session.getReadableDuration());
        durText.setTextColor(getColor(R.color.primary));
        durText.setTextSize(15);
        durText.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(durText);

        card.addView(content);
        return card;
    }

    private String formatSessionType(String type) {
        if (type == null) return "Focus Session";
        switch (type) {
            case "pomodoro": return "🍅 Pomodoro";
            case "deep_work": return "💪 Deep Work";
            case "study": return "📚 Study";
            default: return "⏱ Custom Session";
        }
    }

    private int calculateStreak() {
        List<Long> days = sessionDao.getCompletedSessionDays();
        if (days == null || days.isEmpty()) return 0;

        long todayKey = System.currentTimeMillis() / 86400000L;
        int streak = 0;

        for (int i = 0; i < days.size(); i++) {
            long expectedDay = todayKey - i;
            if (days.get(i) == expectedDay) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private long getStartOfDayMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String formatDuration(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ─── Permissions ────────────────────────────────────────
    private void requestAllPermissions() {
        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, getString(R.string.grant_usage_access), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST);
        }

        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, getString(R.string.enable_accessibility), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        startService(new Intent(this, ForegroundMonitorService.class));
    }

    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, appInfo.uid, appInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAccessibilityEnabled() {
        int enabled = 0;
        String service = getPackageName() + "/com.example.neurafuse.services.ForegroundMonitorService";
        try {
            enabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (enabled == 1) {
            String val = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return val != null && val.contains(service);
        }
        return false;
    }
}
