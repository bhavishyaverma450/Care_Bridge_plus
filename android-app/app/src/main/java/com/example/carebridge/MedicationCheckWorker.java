package com.example.carebridge;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.telephony.SmsManager;
import java.util.Locale;

public class MedicationCheckWorker extends Worker {
    private static final String TAG = "MedicationCheckWorker";

    public MedicationCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            return Result.failure();
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return Result.failure();
        }

        db.child("users").child(userId).child("medicines").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Medicine medicine = data.getValue(Medicine.class);
                    if (medicine != null && !medicine.isTaken()) {
                        Intent intent = new Intent(getApplicationContext(), ReminderBroadcastReceiver.class);
                        intent.putExtra("type", "medication");
                        intent.putExtra("pillName", medicine.getName());
                        intent.putExtra("pillTime", medicine.getTime());
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                getApplicationContext(), medicine.getId().hashCode(), intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );

                        try {
                            // Check for exact alarm permission (Android 12+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                Log.e(TAG, "Exact alarm permission not granted");
                                return;
                            }
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    medicine.getTimestamp(),
                                    pendingIntent
                            );
                            Log.d(TAG, "Scheduled medication reminder for " + medicine.getName() + " at " + medicine.getTime());
                            checkMissedMedicine(medicine, db, userId);
                        } catch (SecurityException e) {
                            Log.e(TAG, "SecurityException scheduling reminder", e);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching medicines", error.toException());
                // Return failure to WorkManager
                // Note: Cannot return directly in ValueEventListener, handled by doWork
            }
        });

        return Result.success();
    }

    private Result checkMissedMedicine(Medicine medicine, DatabaseReference db, String userId) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > medicine.getTimestamp() + 30 * 60 * 1000) { // 30 minutes past reminder
            // Get location
            String locationDetails = getLocationDetails();
            String message = String.format(
                    getApplicationContext().getString(R.string.missed_medicine_text),
                    medicine.getName(), medicine.getTime()
            ) + (locationDetails != null ? "\nLocation: " + locationDetails : "");

            db.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String emergencyContact1 = snapshot.child("emergencyContact1").getValue(String.class);
                    String emergencyContact2 = snapshot.child("emergencyContact2").getValue(String.class);
                    String doctorUid = snapshot.child("doctorUid").getValue(String.class);

                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        SmsManager smsManager = SmsManager.getDefault();
                        if (emergencyContact1 != null && !emergencyContact1.isEmpty()) {
                            try {
                                smsManager.sendTextMessage(emergencyContact1, null, message, null, null);
                                Log.d(TAG, "SMS sent to emergencyContact1: " + emergencyContact1);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to send SMS to emergencyContact1: " + e.getMessage());
                            }
                        } else if (emergencyContact2 != null && !emergencyContact2.isEmpty()) {
                            try {
                                smsManager.sendTextMessage(emergencyContact2, null, message, null, null);
                                Log.d(TAG, "SMS sent to emergencyContact2: " + emergencyContact2);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to send SMS to emergencyContact2: " + e.getMessage());
                            }
                        }

                        if (doctorUid != null) {
                            db.child("users").child(doctorUid).child("phone").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String doctorPhone = snapshot.getValue(String.class);
                                    if (doctorPhone != null && !doctorPhone.isEmpty()) {
                                        try {
                                            smsManager.sendTextMessage(doctorPhone, null, message, null, null);
                                            Log.d(TAG, "SMS sent to doctor: " + doctorPhone);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to send SMS to doctor: " + e.getMessage());
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Failed to fetch doctor phone", error.toException());
                                }
                            });
                        }
                    } else {
                        Log.e(TAG, "SMS permission not granted");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch contacts", error.toException());
                }
            });
            return Result.success();
        }
        return Result.success();
    }

    private String getLocationDetails() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationService.getLastKnownLocation(getApplicationContext());
            if (location != null) {
                return String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f", location.getLatitude(), location.getLongitude());
            }
        }
        return null;
    }
}