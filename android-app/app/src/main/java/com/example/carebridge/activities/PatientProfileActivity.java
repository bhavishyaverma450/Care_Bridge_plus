package com.example.carebridge.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carebridge.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PatientProfileActivity extends AppCompatActivity {
    private TextView profileTitle, nameText, diseaseText, appointmentText;
    private String patientUid, patientName, diseaseName, doctorUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_profile);

    }

    private void fetchLatestAppointment() {
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments")
                .child(doctorUid);

        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String latestAppointmentInfo = "Next Appointment: Not scheduled";
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    String appointmentPatientUid = appointmentSnapshot.child("patientUid").getValue(String.class);
                    if (appointmentPatientUid != null && appointmentPatientUid.equals(patientUid)) {
                        String date = appointmentSnapshot.child("date").getValue(String.class);
                        String time = appointmentSnapshot.child("time").getValue(String.class);
                        String reason = appointmentSnapshot.child("reason").getValue(String.class);
                        latestAppointmentInfo = "Next Appointment: " + (date != null ? date : "N/A") + ", " +
                                (time != null ? time : "N/A") + "\nReason: " + (reason != null ? reason : "N/A");
                        break; // Take the first matching appointment (latest by database order)
                    }
                }
                appointmentText.setText(latestAppointmentInfo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                appointmentText.setText("Next Appointment: Error fetching data");
            }
        });
    }
}