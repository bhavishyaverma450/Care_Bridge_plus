package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText otpInput;
    private Button verifyButton;
    private String verificationId;
    private String phoneNumber;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize UI elements
        otpInput = findViewById(R.id.otp_input);
        verifyButton = findViewById(R.id.verifyButton);

        // Get intent extras
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        role = getIntent().getStringExtra("role");

        if (TextUtils.isEmpty(verificationId) || TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Invalid verification data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verify button
        verifyButton.setOnClickListener(v -> {
            String otp = otpInput.getText().toString().trim();
            if (TextUtils.isEmpty(otp)) {
                otpInput.setError("Enter OTP");
                return;
            }
            verifyOtp(otp);
        });
    }

    private void verifyOtp(String otp) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser() != null ? task.getResult().getUser().getUid() : null;
                        if (userId == null) {
                            Toast.makeText(OtpVerificationActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                        userRef.child("phone").setValue(phoneNumber);
                        userRef.child("role").setValue(role != null ? role : "Patient")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(OtpVerificationActivity.this, "Verification successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(OtpVerificationActivity.this, AboutUserActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(OtpVerificationActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            otpInput.setError("Invalid OTP");
                        } else {
                            Toast.makeText(OtpVerificationActivity.this, "Verification failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}