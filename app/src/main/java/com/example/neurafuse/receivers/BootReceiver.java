package com.example.neurafuse.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.neurafuse.AlarmReceiver;
import com.example.neurafuse.widget.FocusTaskWidgetProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "NeuraFuseTasks";
    private static final String TASK_LIST_KEY = "task_list";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String data = prefs.getString(TASK_LIST_KEY, "[]");
                JSONArray arr = new JSONArray(data);

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    long id = o.optLong("id", -1);
                    long start = o.optLong("startMillis", 0);
                    long end = o.optLong("endMillis", 0);

                    long now = System.currentTimeMillis();
                    if (end <= now) continue; // expired — skip

                    // Re-schedule alarms that were lost on reboot
                    com.example.neurafuse.AddTaskActivity helper = new com.example.neurafuse.AddTaskActivity();
                    com.example.neurafuse.helpers.TaskScheduler.scheduleAlarm(context, id, start, AlarmReceiver.ACTION_START);
                    com.example.neurafuse.helpers.TaskScheduler.scheduleAlarm(context, id, end, AlarmReceiver.ACTION_END);
                }

                FocusTaskWidgetProvider.refreshAllWidgets(context);
                Log.i("BootReceiver", "✅ Tasks rescheduled after reboot");

            } catch (Exception e) {
                Log.e("BootReceiver", "Failed to reload tasks: " + e.getMessage());
            }
        }
    }
}
