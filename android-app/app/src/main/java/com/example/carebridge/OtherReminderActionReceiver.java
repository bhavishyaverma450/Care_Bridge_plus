package com.example.carebridge;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;

public class OtherReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "OtherReminderActionReceiver";
    private static final long SNOOZE_DELAY_MS = 5 * 60 * 1000; // 5 minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String reminderId = intent.getStringExtra("reminderId");

        if (reminderId == null) {
            Log.e(TAG, "Missing reminderId in intent");
            return;
        }

        if ("com.example.carebridge.DISMISS_REMINDER".equals(action)) {
            // User clicked "Dismiss" - cancel the notification
            cancelNotification(context, reminderId.hashCode());
            Log.d(TAG, "Other reminder dismissed: " + reminderId);
        } else if ("com.example.carebridge.SNOOZE_REMINDER".equals(action)) {
            // User clicked "Snooze" - reschedule reminder in 5 minutes
            String title = intent.getStringExtra("title");
            String reason = intent.getStringExtra("reason");
            String notes = intent.getStringExtra("notes");

            if (title != null) {
                // Cancel current notification
                cancelNotification(context, reminderId.hashCode());

                // Create new intent for the snoozed reminder
                Intent snoozeReminderIntent = new Intent(context, ReminderBroadcastReceiver.class);
                snoozeReminderIntent.putExtra("reminderType", "other");
                snoozeReminderIntent.putExtra("reminderId", reminderId);
                snoozeReminderIntent.putExtra("title", title);
                snoozeReminderIntent.putExtra("reason", reason);
                snoozeReminderIntent.putExtra("notes", notes);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminderId.hashCode(),
                        snoozeReminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Schedule the snoozed reminder
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    long triggerTime = SystemClock.elapsedRealtime() + SNOOZE_DELAY_MS;
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Reminder snoozed for 5 minutes: " + title);
                } else {
                    Log.e(TAG, "AlarmManager is null, cannot snooze reminder");
                }
            }
        }
    }

    private void cancelNotification(Context context, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Error canceling notification: " + e.getMessage());
        }
    }
}

