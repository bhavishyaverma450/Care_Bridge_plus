package com.example.carebridge;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private String currentMedicineId;

    private int currentSelectedItemId = R.id.nav_home; // Initially set to home

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Request notification permission for reminders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
                Log.d(TAG, "Requesting notification permission");
            } else {
                Log.d(TAG, "Notification permission already granted");
            }
        }

        // Initialize UI elements
        TextView greetingText = findViewById(R.id.tv_greeting);
        Button sosButton = findViewById(R.id.btn_sos);
        EditText searchEditText = findViewById(R.id.et_search);
        searchEditText.clearFocus(); // Prevent focus on load

        TextView pillTimeText = findViewById(R.id.tv_pill_time);
        TextView pillNameText = findViewById(R.id.tv_pill_name);
        TextView pillQuestionText = findViewById(R.id.tv_pill_question);
        Button yesButton = findViewById(R.id.btn_pill_yes);
        Button noButton = findViewById(R.id.btn_pill_no);
        var ref = new Object() {
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        };
        CheckBox walkCheckBox = findViewById(R.id.checkbox_walk);
        CheckBox meditationCheckBox = findViewById(R.id.checkbox_meditation);
        CheckBox yogaCheckBox = findViewById(R.id.checkbox_yoga);
        RecyclerView calendarRecyclerView = findViewById(R.id.rv_calendar);
        RecyclerView appointmentsRecyclerView = findViewById(R.id.rv_doctor_visits);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Load user name for greeting
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) {
                    greetingText.setText(String.format(getString(R.string.greeting), name));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user name: " + error.getMessage());
            }
        });

        // SOS button
        sosButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            } else {
                sendSOS(userRef);
            }
        });

        // Load next medicine
        loadNextMedicine(userRef, pillTimeText, pillNameText);

        // Yes button
        yesButton.setOnClickListener(v -> {
            if (currentMedicineId != null) {
                userRef.child("medicines").child(currentMedicineId).child("taken").setValue(true)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Medicine marked as taken: " + currentMedicineId);
                            Toast.makeText(HomeActivity.this, "Medicine marked as taken", Toast.LENGTH_SHORT).show();
                            loadNextMedicine(userRef, pillTimeText, pillNameText);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to mark medicine as taken: " + e.getMessage());
                            Toast.makeText(HomeActivity.this, "Failed to mark medicine as taken", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // No button
        noButton.setOnClickListener(v -> {
            if (currentMedicineId != null) {
                userRef.child("medicines").child(currentMedicineId).child("taken").setValue(false)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Medicine marked as not taken: " + currentMedicineId);
                            Toast.makeText(HomeActivity.this, "Medicine marked as not taken", Toast.LENGTH_SHORT).show();
                            sendMissedDoseAlert(userRef, pillNameText.getText().toString());
                            loadNextMedicine(userRef, pillTimeText, pillNameText);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to mark medicine as not taken: " + e.getMessage());
                            Toast.makeText(HomeActivity.this, "Failed to mark medicine as not taken", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // Handle notification actions
        String action = getIntent().getAction();
        if ("com.example.carebridge.MEDICINE_TAKEN".equals(action)) {
            String medicineId = getIntent().getStringExtra("medicineId");
            if (medicineId != null) {
                userRef.child("medicines").child(medicineId).child("taken").setValue(true)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Medicine marked as taken from notification: " + medicineId);
                            Toast.makeText(this, "Medicine marked as taken", Toast.LENGTH_SHORT).show();
                            loadNextMedicine(userRef, pillTimeText, pillNameText);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to mark medicine as taken from notification: " + e.getMessage());
                            Toast.makeText(this, "Failed to mark medicine as taken", Toast.LENGTH_SHORT).show();
                        });
            }
        } else if ("com.example.carebridge.MEDICINE_MISSED".equals(action)) {
            String medicineId = getIntent().getStringExtra("medicineId");
            String medicineName = getIntent().getStringExtra("medicineName");
            if (medicineId != null && medicineName != null) {
                userRef.child("medicines").child(medicineId).child("taken").setValue(false)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Medicine marked as not taken from notification: " + medicineId);
                            Toast.makeText(this, "Medicine marked as not taken", Toast.LENGTH_SHORT).show();
                            sendMissedDoseAlert(userRef, medicineName);
                            loadNextMedicine(userRef, pillTimeText, pillNameText);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to mark medicine as not taken from notification: " + e.getMessage());
                            Toast.makeText(this, "Failed to mark medicine as not taken", Toast.LENGTH_SHORT).show();
                        });
            }
        }


        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            ref.bottomNavigationView = findViewById(R.id.bottom_navigation);
            if (hasFocus) {
                ref.bottomNavigationView.setVisibility(View.GONE);
            } else {
                ref.bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });

        // Search bar
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase(Locale.getDefault());
                Log.d(TAG, "Search query: " + query);
                // Placeholder: Implement search filtering for rv_doctor_visits or other content
            }
        });

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            ref.bottomNavigationView = findViewById(R.id.bottom_navigation);

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible
                ref.bottomNavigationView.setVisibility(View.GONE);
            } else {
                // Keyboard is hidden
                ref.bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });

        ref.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            currentSelectedItemId = itemId;

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_records) {
                startActivity(new Intent(HomeActivity.this, AboutUserActivity.class));
                return true;
            } else if (itemId == R.id.nav_reminders) {
                startActivity(new Intent(HomeActivity.this, RemindersActivity.class));
                return true;
            } else if (itemId == R.id.nav_vitals) {
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });


        // FAB chat
        findViewById(R.id.fab_chat).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ChatBookingActivity.class));
        });

        // Initialize calendar and appointments
        setupCalendarAndAppointments(userId, calendarRecyclerView, appointmentsRecyclerView);

        // Initialize activity checkboxes
        setupActivityCheckboxes(userRef, walkCheckBox, meditationCheckBox, yogaCheckBox);

        // Schedule pill reminders
        schedulePillReminders(userRef);
    }

    private void setupCalendarAndAppointments(String userId, RecyclerView calendarRecyclerView, RecyclerView appointmentsRecyclerView) {
        // Initialize calendar RecyclerView
        List<String> dates = generateDateList();
        CalendarAdapter calendarAdapter = new CalendarAdapter(dates, selectedDate -> {
            Log.d(TAG, "Selected date: " + selectedDate);
            loadAppointmentsForDate(userId, selectedDate, appointmentsRecyclerView);
        });
        calendarRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        calendarRecyclerView.setAdapter(calendarAdapter);

        // Default to current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        loadAppointmentsForDate(userId, currentDate, appointmentsRecyclerView);
    }

    private List<String> generateDateList() {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        for (int i = -7; i <= 7; i++) {
            Calendar temp = (Calendar) calendar.clone();
            temp.add(Calendar.DAY_OF_MONTH, i);
            dates.add(sdf.format(temp.getTime()));
        }
        return dates;
    }

    private void loadAppointmentsForDate(String userId, String selectedDate, RecyclerView appointmentsRecyclerView) {
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments_by_patient").child(userId);
        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppointmentModel> appointments = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentModel appointment = ds.getValue(AppointmentModel.class);
                    if (appointment != null && appointment.getDate() != null && appointment.getDate().equals(selectedDate)) {
                        appointments.add(appointment);
                    }
                }
                AppointmentAdapter appointmentAdapter = new AppointmentAdapter(appointments);
                appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this));
                appointmentsRecyclerView.setAdapter(appointmentAdapter);
                Log.d(TAG, "Loaded " + appointments.size() + " appointments for date: " + selectedDate);
                if (appointments.isEmpty()) {
                    Toast.makeText(HomeActivity.this, "No appointments for " + selectedDate, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load appointments: " + error.getMessage());
                Toast.makeText(HomeActivity.this, "Failed to load appointments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActivityCheckboxes(DatabaseReference userRef, CheckBox walkCheckBox, CheckBox meditationCheckBox, CheckBox yogaCheckBox) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        DatabaseReference activitiesRef = userRef.child("activities").child(currentDate);

        // Load current activity statuses
        activitiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean walkDone = snapshot.child("walkDone").getValue(Boolean.class);
                Boolean meditationDone = snapshot.child("meditationDone").getValue(Boolean.class);
                Boolean yogaDone = snapshot.child("yogaDone").getValue(Boolean.class);

                walkCheckBox.setChecked(walkDone != null && walkDone);
                meditationCheckBox.setChecked(meditationDone != null && meditationDone);
                yogaCheckBox.setChecked(yogaDone != null && yogaDone);
                Log.d(TAG, "Loaded activity statuses for " + currentDate + ": walk=" + walkDone + ", meditation=" + meditationDone + ", yoga=" + yogaDone);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load activities: " + error.getMessage());
                Toast.makeText(HomeActivity.this, "Failed to load activities: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set checkbox listeners
        walkCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activitiesRef.child("walkDone").setValue(isChecked)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Walk activity updated: " + isChecked))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update walk activity: " + e.getMessage());
                        Toast.makeText(HomeActivity.this, "Failed to update walk activity", Toast.LENGTH_SHORT).show();
                    });
        });

        meditationCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activitiesRef.child("meditationDone").setValue(isChecked)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Meditation activity updated: " + isChecked))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update meditation activity: " + e.getMessage());
                        Toast.makeText(HomeActivity.this, "Failed to update meditation activity", Toast.LENGTH_SHORT).show();
                    });
        });

        yogaCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activitiesRef.child("yogaDone").setValue(isChecked)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Yoga activity updated: " + isChecked))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update yoga activity: " + e.getMessage());
                        Toast.makeText(HomeActivity.this, "Failed to update yoga activity", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                sendSOS(userRef);
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            // Handle notification permission result if needed
        }
    }

    private void sendSOS(DatabaseReference userRef) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String contact1Phone = snapshot.child("emergencyContact1").getValue(String.class);
                String contact2Phone = snapshot.child("emergencyContact2").getValue(String.class);
                String name = snapshot.child("name").getValue(String.class);
                String message = name != null ? name + " has triggered an SOS alert!" : "SOS alert from CareBridge user!";

                SmsManager smsManager = SmsManager.getDefault();
                try {
                    if (contact1Phone != null && !contact1Phone.isEmpty()) {
                        smsManager.sendTextMessage(contact1Phone, null, message, null, null);
                    }
                    if (contact2Phone != null && !contact2Phone.isEmpty()) {
                        smsManager.sendTextMessage(contact2Phone, null, message, null, null);
                    }
                    Toast.makeText(HomeActivity.this, "SOS SMS sent", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send SOS SMS: " + e.getMessage());
                    Toast.makeText(HomeActivity.this, "Failed to send SOS SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch emergency contacts: " + error.getMessage());
                Toast.makeText(HomeActivity.this, "Failed to fetch contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNextMedicine(DatabaseReference userRef, TextView pillTimeText, TextView pillNameText) {
        userRef.child("medicines").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentMedicineId = null;
                long earliestTime = Long.MAX_VALUE;
                String earliestName = null;
                String earliestTimeStr = null;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean taken = ds.child("taken").getValue(Boolean.class);
                    String time = ds.child("time").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);
                    if (taken == null || !taken) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
                            long timeMillis = sdf.parse(time).getTime();
                            if (timeMillis < earliestTime) {
                                earliestTime = timeMillis;
                                earliestName = name;
                                earliestTimeStr = time;
                                currentMedicineId = ds.getKey();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse medicine time: " + e.getMessage());
                        }
                    }
                }

                if (currentMedicineId != null) {
                    pillTimeText.setText(earliestTimeStr != null ? earliestTimeStr : getString(R.string.pill_time_default));
                    pillNameText.setText(earliestName != null ? earliestName : getString(R.string.pill_name_default));
                } else {
                    pillTimeText.setText(getString(R.string.pill_time_default));
                    pillNameText.setText(getString(R.string.pill_name_default));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load medicines: " + error.getMessage());
            }
        });
    }

    private void sendMissedDoseAlert(DatabaseReference userRef, String medicineName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            return;
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String contact1Phone = snapshot.child("emergencyContact1").getValue(String.class);
                String contact2Phone = snapshot.child("emergencyContact2").getValue(String.class);
                String name = snapshot.child("name").getValue(String.class);
                String message = name != null ? name + " missed their dose of " + medicineName : "CareBridge user missed their dose of " + medicineName;

                SmsManager smsManager = SmsManager.getDefault();
                try {
                    if (contact1Phone != null && !contact1Phone.isEmpty()) {
                        smsManager.sendTextMessage(contact1Phone, null, message, null, null);
                    }
                    if (contact2Phone != null && !contact2Phone.isEmpty()) {
                        smsManager.sendTextMessage(contact2Phone, null, message, null, null);
                    }
                    Toast.makeText(HomeActivity.this, "Missed dose SMS sent", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send missed dose SMS: " + e.getMessage());
                    Toast.makeText(HomeActivity.this, "Failed to send missed dose SMS", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch emergency contacts: " + error.getMessage());
            }
        });
    }

    private void schedulePillReminders(DatabaseReference userRef) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        userRef.child("medicines").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String medicineId = ds.getKey();
                    String time = ds.child("time").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);
                    if (time == null || name == null) continue;

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(sdf.parse(time));
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                        }

                        Intent intent = new Intent(HomeActivity.this, ReminderBroadcastReceiver.class);
                        intent.putExtra("medicineId", medicineId);
                        intent.putExtra("medicineName", name);
                        intent.putExtra("userId", userRef.getKey());
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                HomeActivity.this,
                                medicineId.hashCode(),
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                                    // Schedule missed dose check after 30 minutes
                                    Calendar missedCalendar = (Calendar) calendar.clone();
                                    missedCalendar.add(Calendar.MINUTE, 30);
                                    Intent missedIntent = new Intent(HomeActivity.this, ReminderBroadcastReceiver.class);
                                    missedIntent.putExtra("medicineId", medicineId);
                                    missedIntent.putExtra("medicineName", name);
                                    missedIntent.putExtra("userId", userRef.getKey());
                                    missedIntent.putExtra("isMissedCheck", true);
                                    PendingIntent missedPendingIntent = PendingIntent.getBroadcast(
                                            HomeActivity.this,
                                            (medicineId + "_missed").hashCode(),
                                            missedIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                    );
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, missedCalendar.getTimeInMillis(), missedPendingIntent);
                                } else {
                                    Log.w(TAG, "Cannot schedule exact alarms, permission not granted");
                                    Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                    startActivity(permissionIntent);
                                }
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                                // Schedule missed dose check after 30 minutes
                                Calendar missedCalendar = (Calendar) calendar.clone();
                                missedCalendar.add(Calendar.MINUTE, 30);
                                Intent missedIntent = new Intent(HomeActivity.this, ReminderBroadcastReceiver.class);
                                missedIntent.putExtra("medicineId", medicineId);
                                missedIntent.putExtra("medicineName", name);
                                missedIntent.putExtra("userId", userRef.getKey());
                                missedIntent.putExtra("isMissedCheck", true);
                                PendingIntent missedPendingIntent = PendingIntent.getBroadcast(
                                        HomeActivity.this,
                                        (medicineId + "_missed").hashCode(),
                                        missedIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                );
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, missedCalendar.getTimeInMillis(), missedPendingIntent);
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "SecurityException when scheduling alarm: " + e.getMessage());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                startActivity(permissionIntent);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to schedule reminder: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load medicines for reminders: " + error.getMessage());
            }
        });
    }
    @Override
    public void onBackPressed() {
        if (currentSelectedItemId != R.id.nav_home) {
            // Set Home as selected in bottom navigation
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            currentSelectedItemId = R.id.nav_home;
        } else {
            // If already on Home, perform default back behavior (exit)
            super.onBackPressed();
        }
    }

}