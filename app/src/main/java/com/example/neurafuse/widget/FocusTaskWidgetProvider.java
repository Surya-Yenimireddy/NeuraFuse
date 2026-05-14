package com.example.neurafuse.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.neurafuse.AddTaskActivity;
import com.example.neurafuse.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class FocusTaskWidgetProvider extends AppWidgetProvider {

    private static final String PREFS_NAME = "NeuraFuseTasks";
    private static final String TASK_LIST_KEY = "task_list";
    private static final String ACTION_DELETE_TASK = "com.example.neurafuse.widget.DELETE_TASK";
    private static final String EXTRA_TASK_INDEX = "task_index";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_focus_task);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(TASK_LIST_KEY, "[]");
        long now = System.currentTimeMillis();

        try {
            JSONArray arr = new JSONArray(json);
            JSONArray valid = new JSONArray();
            StringBuilder display = new StringBuilder();

            // Prepare dynamic text for up to 3 visible tasks
            int visibleCount = Math.min(arr.length(), 3);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                long end = obj.optLong("endMillis");
                long start = obj.optLong("startMillis");

                if (end > now) {
                    valid.put(obj);

                    if (display.length() < 3) {
                        String name = obj.optString("name", "Task");
                        String time = formatTime(start) + " - " + formatTime(end);

                        // Update each text dynamically
                        int taskTextId = context.getResources().getIdentifier("task" + (i + 1), "id", context.getPackageName());
                        int deleteBtnId = context.getResources().getIdentifier("btnDelete" + (i + 1), "id", context.getPackageName());

                        views.setTextViewText(taskTextId, "• " + name + " (" + time + ")");
                        views.setViewVisibility(taskTextId, android.view.View.VISIBLE);
                        views.setViewVisibility(deleteBtnId, android.view.View.VISIBLE);

                        // Set delete button intent
                        Intent delIntent = new Intent(context, FocusTaskWidgetProvider.class);
                        delIntent.setAction(ACTION_DELETE_TASK);
                        delIntent.putExtra(EXTRA_TASK_INDEX, i);
                        PendingIntent delPending = PendingIntent.getBroadcast(
                                context, i, delIntent,
                                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        );
                        views.setOnClickPendingIntent(deleteBtnId, delPending);
                    }
                }
            }

            // Hide unused slots
            for (int j = valid.length(); j < 3; j++) {
                int taskTextId = context.getResources().getIdentifier("task" + (j + 1), "id", context.getPackageName());
                int deleteBtnId = context.getResources().getIdentifier("btnDelete" + (j + 1), "id", context.getPackageName());
                views.setViewVisibility(taskTextId, android.view.View.GONE);
                views.setViewVisibility(deleteBtnId, android.view.View.GONE);
            }

            prefs.edit().putString(TASK_LIST_KEY, valid.toString()).apply();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add Task button
        Intent addIntent = new Intent(context, AddTaskActivity.class);
        PendingIntent addPending = PendingIntent.getActivity(
                context, 0, addIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.btnAddTask, addPending);

        manager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_DELETE_TASK.equals(intent.getAction())) {
            int indexToDelete = intent.getIntExtra(EXTRA_TASK_INDEX, -1);
            if (indexToDelete >= 0) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String json = prefs.getString(TASK_LIST_KEY, "[]");
                try {
                    JSONArray arr = new JSONArray(json);
                    JSONArray newArr = new JSONArray();
                    for (int i = 0; i < arr.length(); i++) {
                        if (i != indexToDelete) newArr.put(arr.getJSONObject(i));
                    }
                    prefs.edit().putString(TASK_LIST_KEY, newArr.toString()).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                refreshAllWidgets(context);
            }
        }
    }

    private static String formatTime(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    public static void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, FocusTaskWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(thisWidget);
        for (int id : ids) updateWidget(context, manager, id);
    }
}
