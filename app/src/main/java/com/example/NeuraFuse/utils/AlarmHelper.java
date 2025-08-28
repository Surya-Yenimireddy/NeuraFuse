package com.example.NeuraFuse.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

import com.example.NeuraFuse.services.ForegroundMonitorService;

public class AlarmHelper {

    public static void scheduleAlarmsFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FocusSchedule", Context.MODE_PRIVATE);
        int hour = prefs.getInt("hour", -1);
        int minute = prefs.getInt("minute", -1);

        if (hour != -1 && minute != -1) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            long triggerTime = calendar.getTimeInMillis();

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent serviceIntent = new Intent(context, ForegroundMonitorService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 1002, serviceIntent, PendingIntent.FLAG_IMMUTABLE);

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}