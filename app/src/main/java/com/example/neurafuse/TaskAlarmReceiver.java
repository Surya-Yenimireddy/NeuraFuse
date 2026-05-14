package com.example.neurafuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.example.neurafuse.services.ForegroundMonitorService;
import com.example.neurafuse.services.MyVpnService;

public class TaskAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskAlarmReceiver";
    public static final String PREFS = "NeuraPrefs";
    public static final String KEY_RUNNING = "isFocusRunning";
    public static final String ACTION_FOCUS_STARTED = "com.example.neurafuse.ACTION_FOCUS_STARTED";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.i(TAG, "Alarm received: starting Focus Mode");

            // 1) Set focus running flag and reset any counters
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean(KEY_RUNNING, true)
                    .putInt("focus_used_chances", 0)
                    .apply();

            // 2) Start VPN service (user must previously have granted VpnService permission)
            Intent vpnIntent = new Intent(context, MyVpnService.class);
            vpnIntent.setAction("android.net.VpnService");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(vpnIntent);
                } else {
                    context.startService(vpnIntent);
                }
            } catch (Exception ex) {
                Log.w(TAG, "Failed to start VPN service: " + ex.getMessage());
            }

            // 3) Start ForegroundMonitorService as foreground service
            Intent monitorIntent = new Intent(context, ForegroundMonitorService.class);
            monitorIntent.setAction(ForegroundMonitorService.ACTION_START_FOREGROUND);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(monitorIntent);
                } else {
                    context.startService(monitorIntent);
                }
            } catch (Exception ex) {
                Log.w(TAG, "Failed to start ForegroundMonitorService: " + ex.getMessage());
            }

            // 4) Notify user quickly
            Toast.makeText(context, "🔥 Focus Mode Activated", Toast.LENGTH_SHORT).show();

            // 5) Broadcast an event so UI can react (optional)
            Intent started = new Intent(ACTION_FOCUS_STARTED);
            context.sendBroadcast(started);

            Log.i(TAG, "Focus Mode started successfully (alarm).");

        } catch (Exception e) {
            Log.e(TAG, "TaskAlarmReceiver onReceive failed: " + e.getMessage(), e);
        }
    }
}
