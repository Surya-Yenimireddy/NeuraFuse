package com.example.neurafuse;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.neurafuse.services.ForegroundMonitorService;
import com.example.neurafuse.widget.FocusTaskWidgetProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_START = "com.example.neurafuse.ACTION_START_TASK";
    public static final String ACTION_END = "com.example.neurafuse.ACTION_END_TASK";

    private static final String PREFS_NAME = "NeuraFuseTasks";
    private static final String TASK_LIST_KEY = "task_list";
    private static final String PREFS_GLOBAL = "NeuraPrefs";
    private static final String KEY_RUNNING = "isFocusRunning";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        long taskId = intent.getLongExtra("taskId", -1);
        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            // Start focus mode
            // Remove expired task from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("NeuraFuseTasks", Context.MODE_PRIVATE);
            try {
                JSONArray arr = new JSONArray(prefs.getString("task_list", "[]"));
                JSONArray updated = new JSONArray();
                long now = System.currentTimeMillis();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject t = arr.getJSONObject(i);
                    if (t.getLong("endMillis") > now) updated.put(t);
                }
                prefs.edit().putString("task_list", updated.toString()).apply();
                FocusTaskWidgetProvider.refreshAllWidgets(context);
            } catch (Exception e) {
                e.printStackTrace();
            }


            FocusTaskWidgetProvider.refreshAllWidgets(context);

        } else if (ACTION_END.equals(action)) {
            // Stop focus mode and remove the finished task by id
            SharedPreferences prefsGlobal = context.getSharedPreferences(PREFS_GLOBAL, Context.MODE_PRIVATE);
            prefsGlobal.edit().putBoolean(KEY_RUNNING, false).apply();

            removeTaskById(context, taskId);
            FocusTaskWidgetProvider.refreshAllWidgets(context);
        }
    }

    private void removeTaskById(Context ctx, long idToRemove) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existing = prefs.getString(TASK_LIST_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(existing);
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long id = o.optLong("id", -1);
                if (id != idToRemove) out.put(o);
            }
            prefs.edit().putString(TASK_LIST_KEY, out.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
