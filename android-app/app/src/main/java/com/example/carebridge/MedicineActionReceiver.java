package com.example.carebridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MedicineActionReceiver extends BroadcastReceiver {
    private static final String TAG = "MedicineActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String medicineId = intent.getStringExtra("medicineId");
        String userId = intent.getStringExtra("userId");

        if (medicineId == null || userId == null) {
            Log.e(TAG, "Missing medicineId or userId in intent");
            return;
        }

        // Get the database reference
        DatabaseReference medicineRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("medicines")
                .child(medicineId);

        // Get current timestamp and date
        long currentTimestamp = System.currentTimeMillis();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        if ("com.example.carebridge.MEDICINE_TAKEN".equals(action)) {
            // User clicked "Yes" - mark medicine as taken
            Map<String, Object> updates = new HashMap<>();
            updates.put("taken", true);
            updates.put("timestamp", currentTimestamp);
            updates.put("lastResponseDate", today); // Add today's date to track response

            medicineRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Medicine marked as taken: " + medicineId);
                        cancelNotification(context, medicineId.hashCode());

                        // Open HomeActivity to refresh the UI
                        Intent homeIntent = new Intent(context, HomeActivity.class);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(homeIntent);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to mark medicine as taken: " + e.getMessage()));
        } else if ("com.example.carebridge.MEDICINE_MISSED".equals(action)) {
            // User clicked "No" - mark medicine as not taken and notify emergency contacts
            String medicineName = intent.getStringExtra("medicineName");

            Map<String, Object> updates = new HashMap<>();
            updates.put("taken", false);
            updates.put("timestamp", currentTimestamp);
            updates.put("lastResponseDate", today); // Add today's date to track response

            medicineRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Medicine marked as not taken: " + medicineId);

                        // Send SMS notification for missed dose
                        if (medicineName != null) {
                            sendMissedDoseAlert(context, userId, medicineName);
                        }

                        cancelNotification(context, medicineId.hashCode());

                        // Open HomeActivity to refresh the UI
                        Intent homeIntent = new Intent(context, HomeActivity.class);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(homeIntent);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to mark medicine as not taken: " + e.getMessage()));
        }
    }

    private void sendMissedDoseAlert(Context context, String userId, String medicineName) {
        // This method will be similar to the existing code in the ReminderBroadcastReceiver
        // but we're separating concerns for better code organization
        FirebaseDatabase.getInstance().getReference("users").child(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String contact1Phone = snapshot.child("emergencyContact1").getValue(String.class);
                    String contact2Phone = snapshot.child("emergencyContact2").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String message = name != null ?
                            name + " missed their dose of " + medicineName :
                            "CareBridge user missed their dose of " + medicineName;

                    try {
                        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
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
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get user data for missed dose alert: " + e.getMessage()));
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

