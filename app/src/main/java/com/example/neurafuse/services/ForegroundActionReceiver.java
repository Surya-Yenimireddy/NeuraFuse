package com.example.neurafuse.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ForegroundActionReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP_FOCUS = "com.example.neurafuse.ACTION_STOP_FOCUS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_STOP_FOCUS.equals(intent.getAction())) {
            try {
                // stop service
                context.stopService(new Intent(context, ForegroundMonitorService.class));
                // also stop FocusModeActivity flags
                context.getSharedPreferences("NeuraPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("isFocusRunning", false).apply();
            } catch (Exception e) {
                Log.w("ForegroundActionReceiver", "stop failed", e);
            }
        }
    }
}
