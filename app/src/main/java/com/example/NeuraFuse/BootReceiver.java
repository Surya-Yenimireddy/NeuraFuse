package com.example.NeuraFuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.NeuraFuse.utils.AlarmHelper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Boot completed detected");
            AlarmHelper.scheduleAlarmsFromPrefs(context);
        }
    }
}