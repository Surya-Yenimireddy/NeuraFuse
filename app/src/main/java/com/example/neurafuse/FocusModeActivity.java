package com.example.neurafuse;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.neurafuse.data.FocusSession;
import com.example.neurafuse.data.NeuraFuseDatabase;
import com.example.neurafuse.services.ForegroundMonitorService;
import com.example.neurafuse.services.MyVpnService;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class FocusModeActivity extends AppCompatActivity {

    private TextView timerDisplay, timerSubtext, focusStatus;
    private TextView preset25, preset60, preset120, presetCustom;
    private Button btnStartFocus, btnStopFocus;
    private View timerGlow;
    private CardView sessionInfoCard;
    private TextView sessionAppsBlocked, sessionType;
    private CountDownTimer countDownTimer;
    private long remainingTimeMillis;
    private long totalDurationMillis;
    private long sessionStartTime;
    private boolean isRunning = false;
    private String currentSessionType = "custom";

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "NeuraPrefs";
    private static final String KEY_RUNNING = "isFocusRunning";
    private static final String KEY_END_TIME = "focusEndTime";

    // Preset selected background
    private int selectedPresetIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Bind views
        timerDisplay = findViewById(R.id.timerDisplay);
        timerSubtext = findViewById(R.id.timerSubtext);
        focusStatus = findViewById(R.id.focusStatus);
        timerGlow = findViewById(R.id.timerGlow);
        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnStopFocus = findViewById(R.id.btnStopFocus);
        sessionInfoCard = findViewById(R.id.sessionInfoCard);
        sessionAppsBlocked = findViewById(R.id.sessionAppsBlocked);
        sessionType = findViewById(R.id.sessionType);

        preset25 = findViewById(R.id.preset25);
        preset60 = findViewById(R.id.preset60);
        preset120 = findViewById(R.id.preset120);
        presetCustom = findViewById(R.id.presetCustom);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Preset clicks
        preset25.setOnClickListener(v -> selectPreset(25, "pomodoro", 0));
        preset60.setOnClickListener(v -> selectPreset(60, "deep_work", 1));
        preset120.setOnClickListener(v -> selectPreset(120, "study", 2));
        presetCustom.setOnClickListener(v -> openCustomTimePicker());

        btnStartFocus.setOnClickListener(v -> {
            if (totalDurationMillis > 0) {
                startFocusSession(totalDurationMillis);
            } else {
                Toast.makeText(this, "Select a duration first", Toast.LENGTH_SHORT).show();
            }
        });

        btnStopFocus.setOnClickListener(v -> stopFocusMode(false));

        // Check for preset from intent (quick launch from home)
        int presetMinutes = getIntent().getIntExtra("preset_minutes", 0);
        String presetType = getIntent().getStringExtra("session_type");
        if (presetMinutes > 0) {
            currentSessionType = presetType != null ? presetType : "custom";
            totalDurationMillis = presetMinutes * 60000L;
            updatePresetDisplay(presetMinutes);
            startFocusSession(totalDurationMillis);
        }

        // Restore running session
        restoreSession();
    }

    private void selectPreset(int minutes, String type, int index) {
        currentSessionType = type;
        totalDurationMillis = minutes * 60000L;
        selectedPresetIndex = index;
        updatePresetDisplay(minutes);

        // Update timer display to show selected time
        int hours = minutes / 60;
        int mins = minutes % 60;
        timerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:00", hours, mins));
        timerSubtext.setText("tap Start to begin");
    }

    private void updatePresetDisplay(int minutes) {
        // Reset all presets
        TextView[] presets = {preset25, preset60, preset120};
        for (TextView p : presets) {
            p.setBackgroundResource(R.drawable.bg_dark_card);
            p.setTextColor(getColor(R.color.text_primary));
        }

        // Highlight selected
        if (selectedPresetIndex >= 0 && selectedPresetIndex < presets.length) {
            presets[selectedPresetIndex].setBackgroundResource(R.drawable.bg_gradient_card);
            presets[selectedPresetIndex].setTextColor(Color.WHITE);
        }
    }

    private void openCustomTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText("Set Focus Duration (Hours:Minutes)")
                .setHour(0)
                .setMinute(30)
                .build();

        picker.show(getSupportFragmentManager(), "custom_picker");
        picker.addOnPositiveButtonClickListener(dialog -> {
            int totalMins = picker.getHour() * 60 + picker.getMinute();
            if (totalMins < 1) {
                Toast.makeText(this, "Minimum 1 minute", Toast.LENGTH_SHORT).show();
                return;
            }
            currentSessionType = "custom";
            totalDurationMillis = totalMins * 60000L;
            selectedPresetIndex = -1;

            int hours = totalMins / 60;
            int mins = totalMins % 60;
            timerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:00", hours, mins));
            timerSubtext.setText("tap Start to begin");

            // Highlight custom button
            presetCustom.setBackgroundResource(R.drawable.bg_gradient_card);
            presetCustom.setTextColor(Color.WHITE);
        });
    }

    private void startFocusSession(long duration) {
        sessionStartTime = System.currentTimeMillis();

        prefs.edit()
                .putBoolean(KEY_RUNNING, true)
                .putLong(KEY_END_TIME, sessionStartTime + duration)
                .apply();

        startTimer(duration);
        startFocusServices();
        showActiveUI();

        Toast.makeText(this, "Focus Mode Activated ✅", Toast.LENGTH_SHORT).show();
    }

    private void startTimer(long duration) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                timerDisplay.setText("00:00:00");
                stopFocusMode(true);

                // Play completion sound
                try {
                    MediaPlayer player = MediaPlayer.create(FocusModeActivity.this,
                            android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
                    player.start();
                    new Handler().postDelayed(() -> {
                        try { player.stop(); player.release(); } catch (Exception ignored) {}
                    }, 8000);
                } catch (Exception ignored) {}

                Toast.makeText(FocusModeActivity.this, getString(R.string.session_complete), Toast.LENGTH_LONG).show();
            }
        }.start();

        isRunning = true;
    }

    private void updateTimerText() {
        int hours = (int) (remainingTimeMillis / 1000) / 3600;
        int minutes = (int) ((remainingTimeMillis / 1000) % 3600) / 60;
        int seconds = (int) (remainingTimeMillis / 1000) % 60;
        timerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void showActiveUI() {
        focusStatus.setText("🟢 Focus Active");
        focusStatus.setTextColor(getColor(R.color.accent));
        timerSubtext.setText("remaining");
        btnStartFocus.setVisibility(View.GONE);
        btnStopFocus.setVisibility(View.VISIBLE);
        sessionInfoCard.setVisibility(View.VISIBLE);
        sessionType.setText("Type: " + formatSessionType(currentSessionType));
        sessionAppsBlocked.setText("Monitoring distracting apps...");

        // Pulse glow animation
        ObjectAnimator anim = ObjectAnimator.ofFloat(timerGlow, "alpha", 0.15f, 0.5f);
        anim.setDuration(1500);
        anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.setRepeatMode(ObjectAnimator.REVERSE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }

    private void showIdleUI() {
        focusStatus.setText("Ready to Focus");
        focusStatus.setTextColor(getColor(R.color.text_secondary));
        timerSubtext.setText("select a duration");
        btnStartFocus.setVisibility(View.VISIBLE);
        btnStopFocus.setVisibility(View.GONE);
        sessionInfoCard.setVisibility(View.GONE);
        timerGlow.clearAnimation();
        timerGlow.setAlpha(0.3f);
    }

    private void stopFocusMode(boolean completed) {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;

        prefs.edit().putBoolean(KEY_RUNNING, false).apply();
        timerDisplay.setText("00:00:00");

        // Stop services
        stopService(new Intent(this, ForegroundMonitorService.class));
        stopService(new Intent(this, MyVpnService.class));

        showIdleUI();

        // Save session to Room DB
        long endTime = System.currentTimeMillis();
        long actualDuration = endTime - sessionStartTime;
        if (sessionStartTime > 0 && actualDuration > 30000) { // Only save sessions > 30 sec
            FocusSession session = new FocusSession(
                    sessionStartTime, endTime, actualDuration, completed, currentSessionType
            );
            Executors.newSingleThreadExecutor().execute(() ->
                    NeuraFuseDatabase.getInstance(this).focusSessionDao().insert(session)
            );
        }
        sessionStartTime = 0;

        if (!completed) {
            Toast.makeText(this, "⏹ Focus Mode Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void startFocusServices() {
        Intent vpnIntent = MyVpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, 100);
        } else {
            startService(new Intent(this, MyVpnService.class));
        }
        startService(new Intent(this, ForegroundMonitorService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startService(new Intent(this, MyVpnService.class));
        }
    }

    private void restoreSession() {
        long endTimeMillis = prefs.getLong(KEY_END_TIME, 0);
        if (prefs.getBoolean(KEY_RUNNING, false)) {
            remainingTimeMillis = endTimeMillis - System.currentTimeMillis();
            if (remainingTimeMillis > 0) {
                sessionStartTime = endTimeMillis - totalDurationMillis;
                startTimer(remainingTimeMillis);
                showActiveUI();
            }
        }
    }

    private String formatSessionType(String type) {
        if (type == null) return "Custom";
        switch (type) {
            case "pomodoro": return "🍅 Pomodoro (25m)";
            case "deep_work": return "💪 Deep Work (1h)";
            case "study": return "📚 Study (2h)";
            default: return "⏱ Custom";
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        long endTime = prefs.getLong(KEY_END_TIME, 0);
        if (prefs.getBoolean(KEY_RUNNING, false)) {
            remainingTimeMillis = endTime - System.currentTimeMillis();
            if (remainingTimeMillis > 0) {
                startTimer(remainingTimeMillis);
                showActiveUI();
            }
        }
    }
}
