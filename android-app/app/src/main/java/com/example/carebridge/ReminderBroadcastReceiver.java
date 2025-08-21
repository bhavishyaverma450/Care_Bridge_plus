package com.example.carebridge;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderBroadcastReceiver";
    private static final String MEDICINE_CHANNEL_ID = "CareBridgeMedicationChannel";
    private static final String OTHER_CHANNEL_ID = "CareBridgeOtherChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if it's a medicine or other reminder
        String reminderType = intent.getStringExtra("reminderType");

        if (reminderType != null && reminderType.equals("medicine")) {
            handleMedicineReminder(context, intent);
        } else if (reminderType != null && reminderType.equals("other")) {
            handleOtherReminder(context, intent);
        } else {
            // For backward compatibility
            handleMedicineReminder(context, intent);
        }
    }

    private void handleMedicineReminder(Context context, Intent intent) {
        String medicineId = intent.getStringExtra("medicineId");
        String medicineName = intent.getStringExtra("medicineName");
        String userId = intent.getStringExtra("userId");
        String dosage = intent.getStringExtra("dosage");
        String dosageUnit = intent.getStringExtra("dosageUnit");
        boolean isMissedCheck = intent.getBooleanExtra("isMissedCheck", false);

        if (medicineId == null || medicineName == null || userId == null) {
            Log.e(TAG, "Missing medicineId, medicineName, or userId in intent");
            return;
        }

        if (!isMissedCheck) {
            // Check if user has already responded to this medicine for today
            checkIfAlreadyResponded(context, userId, medicineId, medicineName, dosage, dosageUnit);
        } else {
            // Check if medicine was missed and send SMS if needed
            FirebaseDatabase.getInstance().getReference("users").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Boolean taken = snapshot.child("medicines").child(medicineId).child("taken").getValue(Boolean.class);
                            if (taken == null || !taken) {
                                String contact1Phone = snapshot.child("emergencyContact1").getValue(String.class);
                                String contact2Phone = snapshot.child("emergencyContact2").getValue(String.class);
                                String name = snapshot.child("name").getValue(String.class);
                                String message = name != null ?
                                        name + " missed their dose of " + medicineName :
                                        "CareBridge user missed their dose of " + medicineName;

                                SmsManager smsManager = SmsManager.getDefault();
                                try {
                                    if (contact1Phone != null && !contact1Phone.isEmpty()) {
                                        smsManager.sendTextMessage(contact1Phone, null, message, null, null);
                                    }
                                    if (contact2Phone != null && !contact2Phone.isEmpty()) {
                                        smsManager.sendTextMessage(contact2Phone, null, message, null, null);
                                    }
                                    Log.d(TAG, "Missed dose SMS sent for " + medicineName);
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to send missed dose SMS: " + e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to check missed dose: " + error.getMessage());
                        }
                    });
        }
    }

    // Check if user has already responded to this medicine for today
    private void checkIfAlreadyResponded(Context context, String userId, String medicineId,
                                         String medicineName, String dosage, String dosageUnit) {
        // Get today's date in the format yyyy-MM-dd
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        DatabaseReference medicineRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("medicines")
                .child(medicineId);

        medicineRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if there's a lastResponseDate and if it's today
                String lastResponseDate = dataSnapshot.child("lastResponseDate").getValue(String.class);
                Boolean hasResponded = lastResponseDate != null && lastResponseDate.equals(today);

                if (!hasResponded) {
                    // User hasn't responded for today, show notification
                    showMedicineNotification(context, medicineId, medicineName, userId, dosage, dosageUnit);
                } else {
                    Log.d(TAG, "User has already responded to " + medicineName + " today (" + today + "). No notification shown.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to check if medicine was already responded to: " + databaseError.getMessage());
                // On error, show notification to be safe
                showMedicineNotification(context, medicineId, medicineName, userId, dosage, dosageUnit);
            }
        });
    }

    // Show the medicine reminder notification
    private void showMedicineNotification(Context context, String medicineId, String medicineName,
                                          String userId, String dosage, String dosageUnit) {
        // Create the notification channel for medicine reminders if needed
        createNotificationChannel(context, MEDICINE_CHANNEL_ID, "Medication Reminders",
                "Notifications for medication reminders", NotificationManager.IMPORTANCE_HIGH);

        // Prepare "Yes" action - medicine taken
        Intent yesIntent = new Intent(context, MedicineActionReceiver.class);
        yesIntent.setAction("com.example.carebridge.MEDICINE_TAKEN");
        yesIntent.putExtra("medicineId", medicineId);
        yesIntent.putExtra("userId", userId);
        PendingIntent yesPendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId.hashCode(),
                yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Prepare "No" action - medicine missed
        Intent noIntent = new Intent(context, MedicineActionReceiver.class);
        noIntent.setAction("com.example.carebridge.MEDICINE_MISSED");
        noIntent.putExtra("medicineId", medicineId);
        noIntent.putExtra("userId", userId);
        noIntent.putExtra("medicineName", medicineName);
        PendingIntent noPendingIntent = PendingIntent.getBroadcast(
                context,
                (medicineId + "_no").hashCode(),
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent to open the app when notification is tapped
        Intent contentIntent = new Intent(context, HomeActivity.class);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                medicineId.hashCode() + 100,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Content text with dosage information if available
        String contentText = "Have you taken your " + medicineName + "?";
        if (dosage != null && !dosage.isEmpty() && dosageUnit != null && !dosageUnit.isEmpty()) {
            contentText += " (" + dosage + " " + dosageUnit + ")";
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MEDICINE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle("Medication Reminder")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_pill, "Yes", yesPendingIntent)
                .addAction(R.drawable.ic_pill, "No", noPendingIntent)
                .setOngoing(true) // Make notification persistent
                .setAutoCancel(false);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(medicineId.hashCode(), builder.build());
            Log.d(TAG, "Medicine reminder notification sent for: " + medicineName);
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted: " + e.getMessage());
        }
    }

    private void handleOtherReminder(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminderId");
        String title = intent.getStringExtra("title");
        String reason = intent.getStringExtra("reason");
        String notes = intent.getStringExtra("notes");

        if (reminderId == null || title == null) {
            Log.e(TAG, "Missing reminderId or title in intent for other reminder");
            return;
        }

        // Create the notification channel for other reminders if needed
        createNotificationChannel(context, OTHER_CHANNEL_ID, "Other Reminders",
                "Notifications for other reminders", NotificationManager.IMPORTANCE_DEFAULT);

        // Intent to open the app when notification is tapped
        Intent contentIntent = new Intent(context, RemindersActivity.class);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                reminderId.hashCode() + 200,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Prepare "Dismiss" action
        Intent dismissIntent = new Intent(context, OtherReminderActionReceiver.class);
        dismissIntent.setAction("com.example.carebridge.DISMISS_REMINDER");
        dismissIntent.putExtra("reminderId", reminderId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.hashCode(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Prepare "Snooze" action
        Intent snoozeIntent = new Intent(context, OtherReminderActionReceiver.class);
        snoozeIntent.setAction("com.example.carebridge.SNOOZE_REMINDER");
        snoozeIntent.putExtra("reminderId", reminderId);
        snoozeIntent.putExtra("title", title);
        snoozeIntent.putExtra("reason", reason);
        snoozeIntent.putExtra("notes", notes);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.hashCode() + 1,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Prepare notification content
        String contentText = reason != null && !reason.isEmpty()
                ? reason
                : "You have a reminder";

        String bigText = contentText;
        if (notes != null && !notes.isEmpty()) {
            bigText += "\nNotes: " + notes;
        }

        // Build the notification with actions
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, OTHER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .addAction(android.R.drawable.ic_popup_reminder, "Snooze (5 min)", snoozePendingIntent)
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(reminderId.hashCode(), builder.build());
            Log.d(TAG, "Other reminder notification sent for: " + title);
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context, String channelId, String name, String description, int importance) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
