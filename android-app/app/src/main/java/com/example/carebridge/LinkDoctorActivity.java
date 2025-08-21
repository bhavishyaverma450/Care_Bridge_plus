package com.example.carebridge;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.google.zxing.Result;
import java.util.List;

public class LinkDoctorActivity extends AppCompatActivity implements BarcodeCallback {
    private static final String TAG = "LinkDoctorActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
    private DecoratedBarcodeView barcodeView;
    private CaptureManager captureManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_doctor);

        // Initialize UI elements
        barcodeView = findViewById(R.id.barcode_scanner);
        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            setResult(RESULT_CANCELED);
            finish();
        });

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeScanner(savedInstanceState);
        }
    }

    private void initializeScanner(Bundle savedInstanceState) {
        Log.d(TAG, "Initializing QR scanner");
        try {
            captureManager = new CaptureManager(this, barcodeView);
            captureManager.initializeFromIntent(getIntent(), savedInstanceState);
            barcodeView.decodeContinuous(this);  // Set the callback to this activity
            captureManager.onResume();  // Start decoding
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize scanner: " + e.getMessage());
            Toast.makeText(this, "Failed to initialize QR scanner", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void barcodeResult(BarcodeResult result) {
        if (result != null) {
            Result rawResult = result.getResult();
            String doctorUid = rawResult.getText();
            Log.d(TAG, "QR scan result: rawText=\"" + (doctorUid != null ? doctorUid : "null") + "\"");
            if (doctorUid != null && !doctorUid.trim().isEmpty()) {
                validateAndLinkDoctor(doctorUid.trim());
            } else {
                Log.w(TAG, "Invalid QR code: null or empty");
                Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show();
                barcodeView.resume();
            }
        }
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {
        // Optional: Handle possible result points if needed
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted, initializing scanner");
                initializeScanner(null);
            } else {
                Log.w(TAG, "Camera permission denied");
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (captureManager != null) {
            Log.d(TAG, "Resuming scanner");
            captureManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (captureManager != null) {
            Log.d(TAG, "Pausing scanner");
            captureManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captureManager != null) {
            Log.d(TAG, "Destroying scanner");
            captureManager.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (captureManager != null) {
            captureManager.onSaveInstanceState(outState);
        }
    }

    private void validateAndLinkDoctor(String doctorUid) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, redirecting to Login_Activity");
            startActivity(new Intent(this, Login_Activity.class));
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String patientUid = currentUser.getUid();
        Log.d(TAG, "Current patientUid: " + patientUid);
        DatabaseReference patientRef = FirebaseDatabase.getInstance().getReference("users").child(patientUid);
        DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference("users").child(doctorUid);

        // Validate doctorUid exists
        Log.d(TAG, "Validating doctorUid: " + doctorUid);
        doctorRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.getValue(String.class);
                    Log.d(TAG, "Doctor role: " + role);
                    if ("doctor".equals(role)) {
                        patientRef.child("doctorUid").setValue(doctorUid);
                        doctorRef.child("patients").child(patientUid).setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Doctor linked successfully, doctorUid: " + doctorUid);
                                    Toast.makeText(LinkDoctorActivity.this, "Doctor linked successfully", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("doctorUid", doctorUid);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to link doctor: " + e.getMessage());
                                    Toast.makeText(LinkDoctorActivity.this, "Failed to link doctor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    barcodeView.resume();
                                });
                    } else {
                        Log.w(TAG, "Invalid doctor QR code: Not a doctor, role: " + role);
                        Toast.makeText(LinkDoctorActivity.this, "Invalid doctor QR code: Not a doctor", Toast.LENGTH_SHORT).show();
                        barcodeView.resume();
                    }
                } else {
                    Log.w(TAG, "Doctor not found for doctorUid: " + doctorUid);
                    Toast.makeText(LinkDoctorActivity.this, "Doctor not found", Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to validate doctor: " + error.getMessage());
                Toast.makeText(LinkDoctorActivity.this, "Failed to validate doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                barcodeView.resume();
            }
        });
    }
}