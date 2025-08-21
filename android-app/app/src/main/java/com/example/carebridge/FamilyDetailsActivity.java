package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FamilyDetailsActivity extends AppCompatActivity {
    private static final String TAG = "FamilyDetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_details);

        // Initialize UI elements
        EditText addressEditText = findViewById(R.id.addressEditText);
        EditText emergencyContact1NameEditText = findViewById(R.id.emergencyContact1NameEditText);
        EditText emergencyContact1PhoneEditText = findViewById(R.id.emergencyContact1PhoneEditText);
        EditText emergencyContact1RelationEditText = findViewById(R.id.emergencyContact1RelationEditText);
        EditText emergencyContact2NameEditText = findViewById(R.id.emergencyContact2NameEditText);
        EditText emergencyContact2PhoneEditText = findViewById(R.id.emergencyContact2PhoneEditText);
        EditText emergencyContact2RelationEditText = findViewById(R.id.emergencyContact2RelationEditText);
        Button saveButton = findViewById(R.id.saveButton);
        Button nextButton = findViewById(R.id.nextButton);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Load family details
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String address = snapshot.child("address").getValue(String.class);
                String contact1Name = snapshot.child("emergencyContact1Name").getValue(String.class);
                String contact1Phone = snapshot.child("emergencyContact1").getValue(String.class);
                String contact1Relation = snapshot.child("relation1").getValue(String.class);
                String contact2Name = snapshot.child("emergencyContact2Name").getValue(String.class);
                String contact2Phone = snapshot.child("emergencyContact2").getValue(String.class);
                String contact2Relation = snapshot.child("relation2").getValue(String.class);
                if (address != null) addressEditText.setText(address);
                if (contact1Name != null) emergencyContact1NameEditText.setText(contact1Name);
                if (contact1Phone != null) emergencyContact1PhoneEditText.setText(contact1Phone);
                if (contact1Relation != null) emergencyContact1RelationEditText.setText(contact1Relation);
                if (contact2Name != null) emergencyContact2NameEditText.setText(contact2Name);
                if (contact2Phone != null) emergencyContact2PhoneEditText.setText(contact2Phone);
                if (contact2Relation != null) emergencyContact2RelationEditText.setText(contact2Relation);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load family details: " + error.getMessage());
                Toast.makeText(FamilyDetailsActivity.this, "Failed to load details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Save button
        saveButton.setOnClickListener(v -> {
            saveFamilyDetails(userRef, addressEditText, emergencyContact1NameEditText, emergencyContact1PhoneEditText, emergencyContact1RelationEditText, emergencyContact2NameEditText, emergencyContact2PhoneEditText, emergencyContact2RelationEditText, false);
        });

        // Next button
        nextButton.setOnClickListener(v -> {
            saveFamilyDetails(userRef, addressEditText, emergencyContact1NameEditText, emergencyContact1PhoneEditText, emergencyContact1RelationEditText, emergencyContact2NameEditText, emergencyContact2PhoneEditText, emergencyContact2RelationEditText, true);
        });
    }

    private void saveFamilyDetails(DatabaseReference userRef, EditText addressEditText, EditText emergencyContact1NameEditText, EditText emergencyContact1PhoneEditText, EditText emergencyContact1RelationEditText, EditText emergencyContact2NameEditText, EditText emergencyContact2PhoneEditText, EditText emergencyContact2RelationEditText, boolean navigateToMedicalRecords) {
        String address = addressEditText.getText().toString().trim();
        String contact1Name = emergencyContact1NameEditText.getText().toString().trim();
        String contact1Phone = emergencyContact1PhoneEditText.getText().toString().trim();
        String contact1Relation = emergencyContact1RelationEditText.getText().toString().trim();
        String contact2Name = emergencyContact2NameEditText.getText().toString().trim();
        String contact2Phone = emergencyContact2PhoneEditText.getText().toString().trim();
        String contact2Relation = emergencyContact2RelationEditText.getText().toString().trim();

        userRef.child("address").setValue(address);
        userRef.child("emergencyContact1Name").setValue(contact1Name);
        userRef.child("emergencyContact1").setValue(contact1Phone);
        userRef.child("relation1").setValue(contact1Relation);
        userRef.child("emergencyContact2Name").setValue(contact2Name);
        userRef.child("emergencyContact2").setValue(contact2Phone);
        userRef.child("relation2").setValue(contact2Relation)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Family details saved successfully");
                    Toast.makeText(FamilyDetailsActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
                    if (navigateToMedicalRecords) {
                        Intent intent = new Intent(FamilyDetailsActivity.this, MedicalRecordsActivity.class);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save family details: " + e.getMessage());
                    Toast.makeText(FamilyDetailsActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                });
    }
}