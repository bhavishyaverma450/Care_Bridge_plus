package com.example.carebridge;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookAppointmentActivity extends AppCompatActivity {
    private static final String TAG = "BookAppointmentActivity";
    private CalendarView calendarView;
    private TimePicker timePicker;
    private EditText reason;
    private Button submit;
    private String selectedDate;
    private String doctorUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        calendarView = findViewById(R.id.calendarView);
        timePicker = findViewById(R.id.timePicker);
        reason = findViewById(R.id.reason);
        submit = findViewById(R.id.submit);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        String patientUid = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(patientUid);

        // Fetch doctor UID
        userRef.child("doctorUid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorUid = snapshot.getValue(String.class);
                if (doctorUid == null) {
                    Log.w(TAG, "No doctor linked");
                    Toast.makeText(BookAppointmentActivity.this, "No doctor linked", Toast.LENGTH_SHORT).show();
                    submit.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch doctor: " + error.getMessage());
                Toast.makeText(BookAppointmentActivity.this, "Failed to fetch doctor", Toast.LENGTH_SHORT).show();
            }
        });

        // Set calendar date listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            selectedDate = sdf.format(calendar.getTime());
        });

        // Submit button listener
        submit.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedDate)) {
                Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(reason.getText().toString().trim())) {
                reason.setError("Reason is required");
                return;
            }
            String time = String.format(Locale.US, "%02d:%02d", timePicker.getHour(), timePicker.getMinute());
            bookAppointment(selectedDate, time, reason.getText().toString().trim());
        });
    }

    private void bookAppointment(String date, String time, String reasonText) {
        String patientUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (patientUid == null) {
            Log.w(TAG, "User not authenticated");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        String appointmentId = db.push().getKey();
        if (appointmentId == null) {
            Log.w(TAG, "Failed to generate appointment ID");
            Toast.makeText(this, "Failed to generate appointment ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split date for Firebase path (day/month/year)
        String[] dateParts = date.split("-");
        if (dateParts.length != 3) {
            Log.w(TAG, "Invalid date format: " + date);
            Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return;
        }
        String year = dateParts[0];
        String month = dateParts[1];
        String day = dateParts[2];

        // Check for conflicts
        db.child("appointments").child(doctorUid).child(day).child(month).child(year).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String existingTime = ds.child("time").getValue(String.class);
                    if (time.equals(existingTime)) {
                        Log.w(TAG, "Time slot already booked: " + time);
                        Toast.makeText(BookAppointmentActivity.this, "Time slot already booked", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // Fetch doctor name
                db.child("users").child(doctorUid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String doctorName = snapshot.getValue(String.class);
                        AppointmentModel appointment = new AppointmentModel(appointmentId, date, time, reasonText, patientUid, doctorName != null ? doctorName : "Doctor");
                        appointment.setDoctorUid(doctorUid); // Set doctorUid

                        // Save to appointments and appointments_by_patient
                        Log.d(TAG, "Booking appointment: ID=" + appointmentId + ", Path=appointments/" + doctorUid + "/" + day + "/" + month + "/" + year + "/" + appointmentId);
                        db.child("appointments").child(doctorUid).child(day).child(month).child(year).child(appointmentId).setValue(appointment)
                                .addOnSuccessListener(aVoid -> {
                                    db.child("appointments_by_patient").child(patientUid).child(appointmentId).setValue(appointment)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "Appointment booked successfully: " + appointmentId);
                                                Toast.makeText(BookAppointmentActivity.this, "Appointment booked", Toast.LENGTH_SHORT).show();
                                                scheduleAppointmentReminder(appointment);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Failed to book patient appointment: " + e.getMessage());
                                                Toast.makeText(BookAppointmentActivity.this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to book appointment: " + e.getMessage());
                                    Toast.makeText(BookAppointmentActivity.this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch doctor name: " + error.getMessage());
                        Toast.makeText(BookAppointmentActivity.this, "Failed to fetch doctor name", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check conflicts: " + error.getMessage());
                Toast.makeText(BookAppointmentActivity.this, "Failed to check conflicts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scheduleAppointmentReminder(AppointmentModel appointment) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager unavailable");
            Toast.makeText(this, "AlarmManager unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission not granted");
            Toast.makeText(this, "Exact alarm permission not granted", Toast.LENGTH_SHORT).show();
            Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(permissionIntent);
            return;
        }

        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        intent.putExtra("type", "appointment");
        intent.putExtra("date", appointment.getDate());
        intent.putExtra("time", appointment.getTime());
        intent.putExtra("name", appointment.getDoctor());
        String appointmentId = appointment.getAppointmentId();
        if (appointmentId == null) {
            Log.w(TAG, "Invalid appointment ID");
            Toast.makeText(this, "Invalid appointment ID", Toast.LENGTH_SHORT).show();
            return;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, appointmentId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            String dateTime = appointment.getDate() + " " + appointment.getTime();
            calendar.setTime(sdf.parse(dateTime));
            calendar.add(Calendar.MINUTE, -30); // 30 minutes before
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
            Log.d(TAG, "Scheduled reminder for appointment: " + appointmentId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminder: " + e.getMessage());
            Toast.makeText(this, "Failed to schedule reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}