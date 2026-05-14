package com.example.neurafuse;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.neurafuse.data.FocusSession;
import com.example.neurafuse.data.FocusSessionDao;
import com.example.neurafuse.data.NeuraFuseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SessionHistoryActivity extends AppCompatActivity {

    private LinearLayout sessionListContainer;
    private TextView totalTimeValue, weekSessionsValue, emptyState;
    private FocusSessionDao sessionDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_history);

        sessionDao = NeuraFuseDatabase.getInstance(this).focusSessionDao();

        sessionListContainer = findViewById(R.id.sessionListContainer);
        totalTimeValue = findViewById(R.id.totalTimeValue);
        weekSessionsValue = findViewById(R.id.weekSessionsValue);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {
        // Observe sessions via LiveData
        sessionDao.getAllSessions().observe(this, sessions -> {
            sessionListContainer.removeAllViews();
            if (sessions == null || sessions.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                return;
            }
            emptyState.setVisibility(View.GONE);

            for (FocusSession s : sessions) {
                sessionListContainer.addView(createSessionCard(s));
            }
        });

        // Load summary stats
        Executors.newSingleThreadExecutor().execute(() -> {
            Calendar weekStart = Calendar.getInstance();
            weekStart.add(Calendar.DAY_OF_MONTH, -7);
            long weekMs = weekStart.getTimeInMillis();

            long totalWeek = sessionDao.getTotalFocusTimeThisWeek(weekMs);
            int sessionsWeek = sessionDao.getCompletedSessionsThisWeek(weekMs);

            runOnUiThread(() -> {
                totalTimeValue.setText(formatDuration(totalWeek));
                weekSessionsValue.setText(String.valueOf(sessionsWeek));
            });
        });
    }

    private View createSessionCard(FocusSession session) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.parseColor("#1A1D35"));
        card.setRadius(dpToPx(14));
        card.setCardElevation(0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        content.setGravity(Gravity.CENTER_VERTICAL);

        // Status indicator
        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(10));
        dotParams.setMargins(0, 0, dpToPx(14), 0);
        dot.setLayoutParams(dotParams);
        dot.setBackgroundColor(session.completed ? Color.parseColor("#00E5A0") : Color.parseColor("#FF5C5C"));
        content.addView(dot);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        info.setLayoutParams(infoParams);

        TextView typeText = new TextView(this);
        typeText.setText(formatType(session.sessionType));
        typeText.setTextColor(Color.WHITE);
        typeText.setTextSize(15);
        typeText.setTypeface(null, Typeface.BOLD);
        info.addView(typeText);

        TextView dateText = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        dateText.setText(sdf.format(new Date(session.startTime)));
        dateText.setTextColor(Color.parseColor("#6B6F8D"));
        dateText.setTextSize(12);
        info.addView(dateText);

        TextView statusText = new TextView(this);
        statusText.setText(session.completed ? "✅ Completed" : "❌ Cancelled");
        statusText.setTextColor(session.completed ? Color.parseColor("#00E5A0") : Color.parseColor("#FF5C5C"));
        statusText.setTextSize(12);
        info.addView(statusText);

        content.addView(info);

        // Duration
        TextView durText = new TextView(this);
        durText.setText(session.getReadableDuration());
        durText.setTextColor(Color.parseColor("#4F6EF7"));
        durText.setTextSize(18);
        durText.setTypeface(null, Typeface.BOLD);
        content.addView(durText);

        card.addView(content);
        return card;
    }

    private String formatType(String type) {
        if (type == null) return "Focus Session";
        switch (type) {
            case "pomodoro": return "🍅 Pomodoro";
            case "deep_work": return "💪 Deep Work";
            case "study": return "📚 Study";
            default: return "⏱ Custom Session";
        }
    }

    private String formatDuration(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        else return minutes + "m";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
