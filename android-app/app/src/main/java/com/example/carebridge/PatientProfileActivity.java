package com.example.carebridge;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class PatientProfileActivity extends AppCompatActivity {
    private static final String TAG = "PatientProfileActivity";
    private TextView profileNameTextView, profileEmailTextView, profileAddressTextView, profilePhoneTextView, profileMedicinesTextView;
    private String patientUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_profile);

        // Initialize UI elements
        profileNameTextView = findViewById(R.id.profileNameTextView);
        profileEmailTextView = findViewById(R.id.profileEmailTextView);
        profileAddressTextView = findViewById(R.id.profileAddressTextView);
        profilePhoneTextView = findViewById(R.id.profilePhoneTextView);
        profileMedicinesTextView = findViewById(R.id.profileMedicinesTextView);

        // Get patientUid from Intent
        patientUid = getIntent().getStringExtra("patientUid");
        if (patientUid == null) {
            Log.e(TAG, "patientUid is null");
            finish();
            return;
        }

        // Load patient data
        loadPatientData();
    }

    private void loadPatientData() {
        DatabaseReference patientRef = FirebaseDatabase.getInstance().getReference("users").child(patientUid);
        patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the entire data as a Map
                    Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                    if (data != null) {
                        String name = (String) data.get("name");
                        String email = (String) data.get("email");
                        String address = (String) data.get("address");
                        String phoneNumber = (String) data.get("phoneNumber");
                        String medicinesStr = "N/A";

                        // Handle medicines as a Map if it exists
                        Object medicines = data.get("medicines");
                        if (medicines instanceof Map) {
                            Map<String, Object> medicinesMap = (Map<String, Object>) medicines;
                            StringBuilder medicinesText = new StringBuilder();
                            for (Map.Entry<String, Object> entry : medicinesMap.entrySet()) {
                                medicinesText.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
                            }
                            if (medicinesText.length() > 0) {
                                medicinesStr = medicinesText.substring(0, medicinesText.length() - 2); // Remove trailing comma
                            }
                        } else if (medicines instanceof String) {
                            medicinesStr = (String) medicines;
                        }

                        profileNameTextView.setText(name != null ? name : "N/A");
                        profileEmailTextView.setText("Email: " + (email != null ? email : "N/A"));
                        profileAddressTextView.setText("Address: " + (address != null ? address : "N/A"));
                        profilePhoneTextView.setText("Phone: " + (phoneNumber != null ? phoneNumber : "N/A"));
                        profileMedicinesTextView.setText("Medicines: " + medicinesStr);
                    } else {
                        Log.e(TAG, "Data is null for patientUid: " + patientUid);
                        finish();
                    }
                } else {
                    Log.e(TAG, "No data found for patientUid: " + patientUid);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load patient data: " + error.getMessage());
                finish();
            }
        });
    }
}