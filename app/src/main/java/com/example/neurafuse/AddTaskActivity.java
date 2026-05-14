package com.example.neurafuse;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neurafuse.widget.FocusTaskWidgetProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private EditText taskNameEditText;
    private TextView selectedDateText, startTimeText, endTimeText;
    private Button btnSelectDate, btnStartTime, btnEndTime, btnSaveTask;

    private Calendar selectedDate = Calendar.getInstance();
    private Calendar startTime = Calendar.getInstance();
    private Calendar endTime = Calendar.getInstance();

    private static final String PREFS = "NeuraFuseTasks";
    private static final String TASK_KEY = "task_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        requestExactAlarmPermission();

        taskNameEditText = findViewById(R.id.taskName);
        selectedDateText = findViewById(R.id.txtSelectedDate);
        startTimeText = findViewById(R.id.txtStartTime);
        endTimeText = findViewById(R.id.txtEndTime);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnStartTime = findViewById(R.id.btnSelectStartTime);
        btnEndTime = findViewById(R.id.btnSelectEndTime);
        btnSaveTask = findViewById(R.id.btnSaveTask);

        prefillDefaults();

        btnSelectDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate.set(year, month, day);
                selectedDateText.setText(String.format("%02d/%02d/%d", day, month + 1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, minute);
                startTimeText.setText(String.format("%02d:%02d", hour, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        btnEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                endTime.set(Calendar.HOUR_OF_DAY, hour);
                endTime.set(Calendar.MINUTE, minute);
                endTimeText.setText(String.format("%02d:%02d", hour, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        btnSaveTask.setOnClickListener(v -> saveTask());
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void prefillDefaults() {
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);
        int month = selectedDate.get(Calendar.MONTH) + 1;
        int year = selectedDate.get(Calendar.YEAR);
        selectedDateText.setText(String.format("%02d/%02d/%d", day, month, year));

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 1);
        startTime = (Calendar) now.clone();
        startTimeText.setText(String.format("%02d:%02d",
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE)));

        Calendar later = (Calendar) now.clone();
        later.add(Calendar.MINUTE, 30);
        endTime = later;
        endTimeText.setText(String.format("%02d:%02d",
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE)));
    }

    private void scheduleTask(long triggerMillis) {
        try {
            Intent intent = new Intent(this, TaskAlarmReceiver.class);
            intent.setAction("com.example.neurafuse.TASK_ALARM");

            int requestCode = (int) (triggerMillis % Integer.MAX_VALUE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                );
                Log.i(TAG, "Alarm scheduled at: " + triggerMillis);
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleTask failed: " + e.getMessage(), e);
            Toast.makeText(this,
                    "Failed to schedule alarm: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void saveTask() {
        try {
            String name = taskNameEditText.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter task name", Toast.LENGTH_SHORT).show();
                return;
            }

            long startMillis = getCombinedTimeMillis(startTime);
            long endMillis = getCombinedTimeMillis(endTime);

            if (endMillis <= startMillis) {
                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject task = new JSONObject();
            task.put("name", name);
            task.put("date", selectedDateText.getText());
            task.put("startMillis", startMillis);
            task.put("endMillis", endMillis);
            task.put("start", startTimeText.getText());
            task.put("end", endTimeText.getText());

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            JSONArray arr = new JSONArray(prefs.getString(TASK_KEY, "[]"));
            arr.put(task);
            prefs.edit().putString(TASK_KEY, arr.toString()).apply();

            FocusTaskWidgetProvider.refreshAllWidgets(this);

            scheduleTask(startMillis);

            Toast.makeText(this, "Task saved & scheduled", Toast.LENGTH_SHORT).show();

            finish();

        } catch (Exception e) {
            Log.e(TAG, "saveTask error: " + e.getMessage(), e);
            Toast.makeText(this,
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private long getCombinedTimeMillis(Calendar time) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
        c.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        c.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
