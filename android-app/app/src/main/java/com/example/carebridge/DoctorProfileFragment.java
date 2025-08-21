package com.example.carebridge;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class DoctorProfileFragment extends Fragment {
    private static final String TAG = "DoctorProfileFragment";
    private ActivityResultLauncher<Intent> phoneVerificationLauncher;
    private ActivityResultLauncher<Intent> changePasswordLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phoneVerificationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> Toast.makeText(getContext(),
                        result.getResultCode() == Activity.RESULT_OK
                                ? "Phone verified successfully"
                                : "Phone verification failed",
                        Toast.LENGTH_SHORT).show());
        changePasswordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> Toast.makeText(getContext(),
                        result.getResultCode() == Activity.RESULT_OK
                                ? "Password changed successfully"
                                : "Password change failed",
                        Toast.LENGTH_SHORT).show());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_profile, container, false);

        // UI elements
        TextView doctorUidTextView = view.findViewById(R.id.doctorUidTextView);
        EditText profileName = view.findViewById(R.id.profileName);
        EditText profileEmail = view.findViewById(R.id.profileEmail);
        EditText profilePhone = view.findViewById(R.id.profilePhone);
        Button showQrBtn = view.findViewById(R.id.showQrBtn);
        Button updateProfile = view.findViewById(R.id.updateProfile);
        Button changePassword = view.findViewById(R.id.changePassword);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        TextView aboutUs = view.findViewById(R.id.aboutUs);
        ImageView qrCodeImage = view.findViewById(R.id.qrCodeImage);

        // Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated, redirecting to Login_Activity");
            startActivity(new Intent(getContext(), Login_Activity.class));
            if (getActivity() != null) getActivity().finish();
            return view;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Load and display profile info
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                if (name != null) profileName.setText(name);
                if (email != null) profileEmail.setText(email);
                if (phone != null) profilePhone.setText(phone);

                doctorUidTextView.setText(String.format(getString(R.string.doctor_uid), userId));

                // Enable long-press to copy UID
                doctorUidTextView.setOnLongClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Doctor UID", userId);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "UID copied to clipboard", Toast.LENGTH_SHORT).show();
                    return true;
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load profile: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // QR Code
        showQrBtn.setOnClickListener(v -> {
            Log.d(TAG, "Generating QR code for userId: " + userId);
            generateQrCode(userId, qrCodeImage);
        });

        // Update Profile
        updateProfile.setOnClickListener(v -> {
            String name = profileName.getText().toString().trim();
            String email = profileEmail.getText().toString().trim();
            String phone = profilePhone.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Log.w(TAG, "Profile update failed: Empty fields");
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            userRef.child("name").setValue(name);
            userRef.child("email").setValue(email);
            userRef.child("phone").setValue(phone)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile updated successfully");
                        Toast.makeText(getContext(), R.string.save_success, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Profile update failed: " + e.getMessage());
                        Toast.makeText(getContext(), R.string.save_failed, Toast.LENGTH_SHORT).show();
                    });
        });

        // Change Password
        changePassword.setOnClickListener(v -> {
            Log.d(TAG, "Launching ChangePasswordActivity");
            Intent intent = new Intent(getContext(), ChangePasswordActivity.class);
            changePasswordLauncher.launch(intent);
        });

        // Logout
        btnLogout.setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                Log.d(TAG, "Clearing shared preferences and logging out");
                context.getSharedPreferences("CareBridgePrefs", Context.MODE_PRIVATE).edit().clear().apply();
            }
            mAuth.signOut();
            startActivity(new Intent(getContext(), Login_Activity.class));
            if (getActivity() != null) getActivity().finish();
        });

        // About Us
        aboutUs.setOnClickListener(v -> {
            Log.d(TAG, "About Us clicked");
            Toast.makeText(getContext(), "About Us clicked", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void generateQrCode(String userId, ImageView qrCodeImage) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            Toast.makeText(getContext(), "Failed to generate QR code: Invalid user ID", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 200, 200);
            qrCodeImage.setImageBitmap(bitmap);
            qrCodeImage.setVisibility(View.VISIBLE);
            Log.d(TAG, "QR code generated successfully for userId: " + userId);
        } catch (WriterException e) {
            Log.e(TAG, "Failed to generate QR code: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
