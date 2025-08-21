package com.example.carebridge;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String CHANNEL_ID = "CareBridgeNotifications";

    private List<AppointmentModel> appointmentList;
    private List<AlertModel> alertList;

    private TextView noAppointmentText, noAlertText;
    private DoctorVisitAdapter appointmentAdapter;
    private AlertAdapter alertAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView appointmentRecycler = view.findViewById(R.id.appointmentRecycler);
        RecyclerView alertRecycler = view.findViewById(R.id.alertRecycler);
        EditText searchEditText = view.findViewById(R.id.searchEditText);
        noAppointmentText = view.findViewById(R.id.noAppointmentText);
        noAlertText = view.findViewById(R.id.noAlertText);

        appointmentList = new ArrayList<>();
        alertList = new ArrayList<>();

        appointmentAdapter = new DoctorVisitAdapter(appointmentList);
        alertAdapter = new AlertAdapter(alertList);

        appointmentRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        appointmentRecycler.setAdapter(appointmentAdapter);

        alertRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        alertRecycler.setAdapter(alertAdapter);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(getContext(), Login_Activity.class));
            if (getActivity() != null) getActivity().finish();
            return view;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Load appointments
        FirebaseDatabase.getInstance().getReference("appointments_by_patient").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        appointmentList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            AppointmentModel appointment = ds.getValue(AppointmentModel.class);
                            if (appointment != null) {
                                appointmentList.add(appointment);
                            }
                        }
                        appointmentAdapter.notifyDataSetChanged();
                        noAppointmentText.setVisibility(appointmentList.isEmpty() ? View.VISIBLE : View.GONE);
                        scheduleAppointmentReminders(appointmentList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load appointments: " + error.getMessage());
                    }
                });

        // Load alerts (from medicines)
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("medicines")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        alertList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String name = ds.child("name").getValue(String.class);
                            String time = ds.child("time").getValue(String.class);
                            Boolean taken = ds.child("taken").getValue(Boolean.class);

                            if (name != null && time != null && (taken == null || !taken)) {
                                AlertModel alert = new AlertModel(UUID.randomUUID().toString(), "Missed medicine: " + name, time, "medicine");
                                alertList.add(alert);
                            }
                        }
                        alertAdapter.notifyDataSetChanged();
                        noAlertText.setVisibility(alertList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load alerts: " + error.getMessage());
                    }
                });

        // Search filtering
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase(Locale.getDefault());
                List<AppointmentModel> filteredList = new ArrayList<>();
                for (AppointmentModel appointment : appointmentList) {
                    String doctor = appointment.getDoctor();
                    String reason = appointment.getReason();
                    if ((doctor != null && doctor.toLowerCase().contains(query)) ||
                            (reason != null && reason.toLowerCase().contains(query))) {
                        filteredList.add(appointment);
                    }
                }
                appointmentAdapter.updateList(filteredList);
            }
        });

        return view;
    }

    private void scheduleAppointmentReminders(List<AppointmentModel> appointments) {
        Context context = getContext();
        if (context == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("CareBridge notification channel");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Schedule reminders
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (AppointmentModel appointment : appointments) {
            String date = appointment.getDate();
            String time = appointment.getTime();
            if (date == null || time == null) continue;

            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(sdf.parse(date + " " + time));
                calendar.add(Calendar.MINUTE, -30); // 30 minutes before

                Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
                intent.putExtra("action", "APPOINTMENT_REMINDER");
                intent.putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
                intent.putExtra("appointmentId", appointment.getAppointmentId());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        appointment.getAppointmentId().hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        Log.w(TAG, "Exact alarm permission not granted");
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to parse appointment date/time: " + e.getMessage());
            }
        }
    }
}
