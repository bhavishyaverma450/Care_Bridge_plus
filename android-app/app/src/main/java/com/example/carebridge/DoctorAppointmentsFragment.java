package com.example.carebridge;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DoctorAppointmentsFragment extends Fragment {
    private static final String TAG = "DoctorAppointmentsFrag";

    private RecyclerView appointmentRecycler;
    private AppointmentAdapter appointmentAdapter;
    private final List<AppointmentModel> appointmentList = new ArrayList<>();
    private TextView noAppointmentsText, noAlertsText;
    private RecyclerView alertRecycler;
    private AlertAdapter alertAdapter;
    private final List<AlertModel> alertList = new ArrayList<>();


    private DatabaseReference database;
    private String doctorUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_appointments, container, false);

        appointmentRecycler = view.findViewById(R.id.appointmentRecycler);
        appointmentRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        appointmentAdapter = new AppointmentAdapter(appointmentList);
        appointmentRecycler.setAdapter(appointmentAdapter);
        noAppointmentsText = view.findViewById(R.id.noAppointmentsText);
        noAlertsText = view.findViewById(R.id.noAlertsText);


        alertRecycler = view.findViewById(R.id.alertRecycler);
        alertRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        alertAdapter = new AlertAdapter(alertList);
        alertRecycler.setAdapter(alertAdapter);


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        doctorUid = mAuth.getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance().getReference();

        fetchAppointments();
        fetchAppointments();
        fetchAlerts();

        return view;
    }

    private void fetchAlerts() {
        alertList.clear();
        noAlertsText.setVisibility(View.GONE);

        DatabaseReference alertRef = database.child("alerts").child(doctorUid);
        alertRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    AlertModel alert = child.getValue(AlertModel.class);
                    if (alert != null) {
                        alertList.add(alert);
                    }
                }

                if (alertList.isEmpty()) {
                    noAlertsText.setVisibility(View.VISIBLE);
                } else {
                    noAlertsText.setVisibility(View.GONE);
                }

                alertAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load alerts: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load alerts", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchAppointments() {
        appointmentList.clear();
        Log.d(TAG, "Fetching appointments for doctor: " + doctorUid);

        DatabaseReference appointmentRef = database.child("appointments").child(doctorUid);
        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot yearSnap) {
                List<DataSnapshot> allAppointmentSnaps = flattenDateStructure(yearSnap);

                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().getTime());

                for (DataSnapshot apptSnap : allAppointmentSnaps) {
                    for (DataSnapshot child : apptSnap.getChildren()) {
                        AppointmentModel appointment = child.getValue(AppointmentModel.class);
                        if (appointment == null || appointment.getDate() == null) continue;

                        String apptDate = appointment.getDate();
                        if (apptDate.compareTo(todayStr) < 0) {
                            // Delete expired appointment
                            Log.d(TAG, "Deleting old appointment: " + appointment.getAppointmentId());
                            child.getRef().removeValue();
                        } else {
                            appointmentList.add(appointment);
                        }
                    }
                }

                if (appointmentList.isEmpty()) {
                    noAppointmentsText.setVisibility(View.VISIBLE);
                } else {
                    noAppointmentsText.setVisibility(View.GONE);
                }

                Collections.sort(appointmentList, Comparator.comparingLong(AppointmentModel::getTimestamp).reversed());
                appointmentAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load appointments: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load appointments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Flatten appointments/{doctorUid}/{day}/{month}/{year}/appointmentId -> list of appointmentId nodes
    private List<DataSnapshot> flattenDateStructure(DataSnapshot root) {
        List<DataSnapshot> results = new ArrayList<>();
        for (DataSnapshot daySnap : root.getChildren()) {
            for (DataSnapshot monthSnap : daySnap.getChildren()) {
                for (DataSnapshot yearSnap : monthSnap.getChildren()) {
                    results.add(yearSnap);
                }
            }
        }
        return results;
    }
}
