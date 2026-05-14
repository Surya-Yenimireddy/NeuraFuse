package com.example.neurafuse.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.neurafuse.AlarmReceiver;

public class TaskScheduler {

    /**
     * Schedules an alarm for starting or ending a focus task.
     * @param ctx context
     * @param taskId unique id of the task
     * @param triggerAtMillis timestamp when alarm should trigger
     * @param action either AlarmReceiver.ACTION_START or ACTION_END
     */
    public static void scheduleAlarm(Context ctx, long taskId, long triggerAtMillis, String action) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.setAction(action);
        intent.putExtra("taskId", taskId);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                (int) (taskId + action.hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        }
    }
}
