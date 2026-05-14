package com.example.neurafuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class FocusStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("taskName");
        long endMillis = intent.getLongExtra("endMillis", 0);

        Toast.makeText(context, "Starting Focus Mode for " + taskName, Toast.LENGTH_LONG).show();

        Intent focusIntent = new Intent(context, FocusModeActivity.class);
        focusIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        focusIntent.putExtra("autoStart", true);
        focusIntent.putExtra("taskName", taskName);
        focusIntent.putExtra("endMillis", endMillis);
        context.startActivity(focusIntent);
    }
}
