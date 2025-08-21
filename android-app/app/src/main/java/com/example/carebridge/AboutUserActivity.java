package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AboutUserActivity extends AppCompatActivity {
    private static final String TAG = "AboutUserActivity";
    private ActivityResultLauncher<Intent> scanQrLauncher;
    private EditText doctorIdEditText;
    private Button linkDoctorByIdButton;
    private Button scanQrButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_user);

        // Initialize UI elements
        EditText nationalIdEditText = findViewById(R.id.nationalIdEditText);
        EditText bloodGroupEditText = findViewById(R.id.bloodGroupEditText);
        EditText phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        EditText hereditaryDiseasesEditText = findViewById(R.id.hereditaryDiseasesEditText);
        EditText allergiesEditText = findViewById(R.id.allergiesEditText);
        EditText conditionsEditText = findViewById(R.id.conditionsEditText);
        EditText caretakerEditText = findViewById(R.id.caretakerEditText);
        doctorIdEditText = findViewById(R.id.doctorIdEditText);
        linkDoctorByIdButton = findViewById(R.id.linkDoctorByIdButton);
        scanQrButton = findViewById(R.id.scanQrButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button nextButton = findViewById(R.id.nextButton);
        Button unlinkDoctorButton = findViewById(R.id.unlinkDoctorButton);

        // Ensure input fields are enabled by default
        doctorIdEditText.setEnabled(true);
        doctorIdEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        linkDoctorByIdButton.setEnabled(true);
        scanQrButton.setEnabled(true);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, redirecting to Login_Activity");
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        String userId = currentUser.getUid();
        Log.d(TAG, "Current user ID: " + userId);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialize ActivityResultLauncher for QR scan
        scanQrLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Log.d(TAG, "QR scan result received: resultCode=" + result.getResultCode());
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String doctorUid = result.getData().getStringExtra("doctorUid");
                Log.d(TAG, "QR scan returned doctorUid: " + doctorUid);
                if (doctorUid != null && !doctorUid.isEmpty()) {
                    linkDoctor(userRef, doctorUid);
                } else {
                    Log.w(TAG, "Invalid QR code data");
                    Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "QR scan cancelled or failed");
                Toast.makeText(this, "QR scan cancelled or failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Load user details
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nationalId = snapshot.child("nationalId").getValue(String.class);
                String bloodGroup = snapshot.child("bloodGroup").getValue(String.class);
                String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);
                String hereditaryDiseases = snapshot.child("hereditaryDiseases").getValue(String.class);
                String allergies = snapshot.child("allergies").getValue(String.class);
                String conditions = snapshot.child("conditions").getValue(String.class);
                String caretaker = snapshot.child("caretaker").getValue(String.class);
                String doctorUid = snapshot.child("doctorUid").getValue(String.class);
                if (nationalId != null) nationalIdEditText.setText(nationalId);
                if (bloodGroup != null) bloodGroupEditText.setText(bloodGroup);
                if (phoneNumber != null) phoneNumberEditText.setText(phoneNumber);
                if (hereditaryDiseases != null) hereditaryDiseasesEditText.setText(hereditaryDiseases);
                if (allergies != null) allergiesEditText.setText(allergies);
                if (conditions != null) conditionsEditText.setText(conditions);
                if (caretaker != null) caretakerEditText.setText(caretaker);
                if (doctorUid != null) {
                    Log.d(TAG, "Existing doctorUid found: " + doctorUid);
                    doctorIdEditText.setText(doctorUid);
                    doctorIdEditText.setEnabled(false);
                    linkDoctorByIdButton.setEnabled(false);
                    scanQrButton.setEnabled(false);
                    unlinkDoctorButton.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG, "No existing doctorUid, enabling input fields");
                    unlinkDoctorButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user details: " + error.getMessage());
                Toast.makeText(AboutUserActivity.this, "Failed to load details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Link doctor by ID
        linkDoctorByIdButton.setOnClickListener(v -> {
            String doctorId = doctorIdEditText.getText().toString().trim();
            Log.d(TAG, "Link Doctor by ID clicked, doctorId: " + doctorId);
            if (doctorId.isEmpty()) {
                Toast.makeText(this, "Please enter a doctor ID", Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if already linked
            userRef.child("doctorUid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.w(TAG, "Doctor already linked: " + snapshot.getValue(String.class));
                        Toast.makeText(AboutUserActivity.this, "A doctor is already linked. Please unlink first.", Toast.LENGTH_SHORT).show();
                    } else {
                        linkDoctor(userRef, doctorId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to check existing doctorUid: " + error.getMessage());
                    Toast.makeText(AboutUserActivity.this, "Failed to check existing doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Scan QR code
        scanQrButton.setOnClickListener(v -> {
            Log.d(TAG, "Scan QR button clicked");
            // Check if already linked
            userRef.child("doctorUid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.w(TAG, "Doctor already linked: " + snapshot.getValue(String.class));
                        Toast.makeText(AboutUserActivity.this, "A doctor is already linked. Please unlink first.", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(AboutUserActivity.this, LinkDoctorActivity.class);
                        scanQrLauncher.launch(intent);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to check existing doctorUid: " + error.getMessage());
                    Toast.makeText(AboutUserActivity.this, "Failed to check existing doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Unlink doctor
        unlinkDoctorButton.setOnClickListener(v -> {
            Log.d(TAG, "Unlink Doctor button clicked");
            String doctorUid = doctorIdEditText.getText().toString().trim();
            if (doctorUid.isEmpty()) {
                Log.w(TAG, "No doctor linked to unlink");
                Toast.makeText(this, "No doctor linked", Toast.LENGTH_SHORT).show();
                return;
            }
            DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference("users").child(doctorUid).child("patients").child(userId);
            userRef.child("doctorUid").removeValue()
                    .addOnSuccessListener(aVoid -> {
                        doctorRef.removeValue();
                        Log.d(TAG, "Doctor unlinked successfully");
                        Toast.makeText(AboutUserActivity.this, "Doctor unlinked successfully", Toast.LENGTH_SHORT).show();
                        doctorIdEditText.setText("");
                        doctorIdEditText.setEnabled(true);
                        linkDoctorByIdButton.setEnabled(true);
                        scanQrButton.setEnabled(true);
                        unlinkDoctorButton.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to unlink doctor: " + e.getMessage());
                        Toast.makeText(AboutUserActivity.this, "Failed to unlink doctor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Save button
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
            String nationalId = nationalIdEditText.getText().toString().trim();
            String bloodGroup = bloodGroupEditText.getText().toString().trim();
            String phoneNumber = phoneNumberEditText.getText().toString().trim();
            String hereditaryDiseases = hereditaryDiseasesEditText.getText().toString().trim();
            String allergies = allergiesEditText.getText().toString().trim();
            String conditions = conditionsEditText.getText().toString().trim();
            String caretaker = caretakerEditText.getText().toString().trim();

            userRef.child("nationalId").setValue(nationalId);
            userRef.child("bloodGroup").setValue(bloodGroup);
            userRef.child("phoneNumber").setValue(phoneNumber);
            userRef.child("hereditaryDiseases").setValue(hereditaryDiseases);
            userRef.child("allergies").setValue(allergies);
            userRef.child("conditions").setValue(conditions);
            userRef.child("caretaker").setValue(caretaker)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User details saved successfully");
                        Toast.makeText(AboutUserActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save user details: " + e.getMessage());
                        Toast.makeText(AboutUserActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                    });
        });

        // Next button
        nextButton.setOnClickListener(v -> {
            Log.d(TAG, "Next button clicked");
            String nationalId = nationalIdEditText.getText().toString().trim();
            String bloodGroup = bloodGroupEditText.getText().toString().trim();
            String phoneNumber = phoneNumberEditText.getText().toString().trim();
            String hereditaryDiseases = hereditaryDiseasesEditText.getText().toString().trim();
            String allergies = allergiesEditText.getText().toString().trim();
            String conditions = conditionsEditText.getText().toString().trim();
            String caretaker = caretakerEditText.getText().toString().trim();

            userRef.child("nationalId").setValue(nationalId);
            userRef.child("bloodGroup").setValue(bloodGroup);
            userRef.child("phoneNumber").setValue(phoneNumber);
            userRef.child("hereditaryDiseases").setValue(hereditaryDiseases);
            userRef.child("allergies").setValue(allergies);
            userRef.child("conditions").setValue(conditions);
            userRef.child("caretaker").setValue(caretaker)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User details saved successfully");
                        Toast.makeText(AboutUserActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AboutUserActivity.this, FamilyDetailsActivity.class));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save user details: " + e.getMessage());
                        Toast.makeText(AboutUserActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void linkDoctor(DatabaseReference userRef, String doctorUid) {
        if (doctorUid == null || doctorUid.isEmpty()) {
            Log.w(TAG, "Doctor ID is null or empty");
            Toast.makeText(this, "Doctor ID cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference("users").child(doctorUid);
        Log.d(TAG, "Validating doctorUid: " + doctorUid);
        doctorRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.getValue(String.class);
                    Log.d(TAG, "Doctor role: " + role);
                    if ("doctor".equals(role)) {
                        String patientUid = userRef.getKey();
                        if (patientUid == null) {
                            Log.e(TAG, "Patient UID is null");
                            Toast.makeText(AboutUserActivity.this, "Error: User ID is null", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        userRef.child("doctorUid").setValue(doctorUid);
                        doctorRef.child("patients").child(patientUid).setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Doctor linked successfully, doctorUid: " + doctorUid);
                                    Toast.makeText(AboutUserActivity.this, "Doctor linked successfully", Toast.LENGTH_SHORT).show();
                                    doctorIdEditText.setEnabled(false);
                                    linkDoctorByIdButton.setEnabled(false);
                                    scanQrButton.setEnabled(false);
                                    findViewById(R.id.unlinkDoctorButton).setVisibility(View.VISIBLE);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to link doctor: " + e.getMessage());
                                    Toast.makeText(AboutUserActivity.this, "Failed to link doctor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "Invalid doctor ID: Not a doctor, role: " + role);
                        Toast.makeText(AboutUserActivity.this, "Invalid doctor ID: Not a doctor", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Doctor not found for doctorUid: " + doctorUid);
                    Toast.makeText(AboutUserActivity.this, "Doctor not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to validate doctor: " + error.getMessage());
                Toast.makeText(AboutUserActivity.this, "Failed to validate doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}