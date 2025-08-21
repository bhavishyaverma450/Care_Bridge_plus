package com.example.carebridge;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicalRecordsActivity extends AppCompatActivity {
    private static final String TAG = "MedicalRecordsActivity";
    private List<Map<String, Object>> medicinesList;
    private MedicalRecordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        // Initialize UI elements
        EditText medicineNameEditText = findViewById(R.id.medicine_name);
        EditText reminderTimeEditText = findViewById(R.id.reminder_time);
        Button addMedicineButton = findViewById(R.id.add_medicine_button);
        RecyclerView medicalRecordsRecyclerView = findViewById(R.id.medicines_recycler_view);
        EditText diseaseDetailsEditText = findViewById(R.id.disease_details);
        SwitchCompat shareWithCaretakerSwitch = findViewById(R.id.share_with_caretaker);
        Button saveButton = findViewById(R.id.save_button);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialize RecyclerView
        medicinesList = new ArrayList<>();
        adapter = new MedicalRecordAdapter(medicinesList);
        medicalRecordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicalRecordsRecyclerView.setAdapter(adapter);

        // Load medicines
        userRef.child("medicines").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int oldSize = medicinesList.size();
                medicinesList.clear();
                int position = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Map<String, Object> medicine = new HashMap<>();
                    medicine.put("id", ds.getKey());
                    medicine.put("name", ds.child("name").getValue(String.class));
                    medicine.put("time", ds.child("time").getValue(String.class));
                    medicine.put("taken", ds.child("taken").getValue(Boolean.class));
                    medicinesList.add(medicine);
                    adapter.notifyItemInserted(position);
                    position++;
                }
                if (oldSize != medicinesList.size()) {
                    adapter.notifyItemRangeChanged(0, medicinesList.size());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load medicines: " + error.getMessage());
                Toast.makeText(MedicalRecordsActivity.this, "Failed to load medicines", Toast.LENGTH_SHORT).show();
            }
        });

        // Time picker for reminder_time
        reminderTimeEditText.setOnClickListener(v -> {
            Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minuteOfDay) -> {
                        String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minuteOfDay);
                        reminderTimeEditText.setText(time);
                    }, hour, minute, true);
            timePickerDialog.show();
        });

        // Add medicine
        addMedicineButton.setOnClickListener(v -> {
            String name = medicineNameEditText.getText().toString().trim();
            String time = reminderTimeEditText.getText().toString().trim();
            if (name.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please enter medicine name and time", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> medicine = new HashMap<>();
            medicine.put("name", name);
            medicine.put("time", time);
            medicine.put("timestamp", System.currentTimeMillis());
            medicine.put("taken", false);

            String medicineId = userRef.child("medicines").push().getKey();
            if (medicineId != null) {
                userRef.child("medicines").child(medicineId).setValue(medicine)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Medicine added successfully");
                            Toast.makeText(MedicalRecordsActivity.this, "Medicine added", Toast.LENGTH_SHORT).show();
                            medicineNameEditText.setText("");
                            reminderTimeEditText.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to add medicine: " + e.getMessage());
                            Toast.makeText(MedicalRecordsActivity.this, "Failed to add medicine", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // Save disease details and share with caretaker
        saveButton.setOnClickListener(v -> {
            String diseaseDetails = diseaseDetailsEditText.getText().toString().trim();
            boolean shareWithCaretaker = shareWithCaretakerSwitch.isChecked();

            userRef.child("conditions").setValue(diseaseDetails);
            userRef.child("caretakerShare").setValue(shareWithCaretaker)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Details saved" + (shareWithCaretaker ? " and shared with caretaker" : ""));
                        Toast.makeText(MedicalRecordsActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save details: " + e.getMessage());
                        Toast.makeText(MedicalRecordsActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                    });
        });
    }
}