package com.example.carebridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.*;

import java.io.InputStream;

public class PatientProfileFragment extends Fragment {

    private EditText profileName, profileEmail, profilePhone;
    private TextView patientUidTextView, aboutUs, linkedDoctorName;
    private Button btnUpdateProfile, btnChangePassword, btnLogout, btnLinkDoctor,btnUnlinkDoctor,btn_setting;
    private DatabaseReference userRef, doctorRef;
    private FirebaseUser currentUser;

    private ActivityResultLauncher<Intent> qrScanLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_profile, container, false);

        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profilePhone = view.findViewById(R.id.profilePhone);
        patientUidTextView = view.findViewById(R.id.patientUidTextView);
        aboutUs = view.findViewById(R.id.aboutUs);
        linkedDoctorName = view.findViewById(R.id.linkedDoctorName);
        btnUpdateProfile = view.findViewById(R.id.updateProfile);
        btnChangePassword = view.findViewById(R.id.changePassword);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnLinkDoctor = view.findViewById(R.id.btnLinkDoctor);

        btnUnlinkDoctor = view.findViewById(R.id.btnUnlinkDoctor);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Hide keyboard by default
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        disableEditText(profileName);
        disableEditText(profileEmail);
        disableEditText(profilePhone);


        profileName.setOnClickListener(v -> enableEditText(profileName));
        profileEmail.setOnClickListener(v -> enableEditText(profileEmail));
        profilePhone.setOnClickListener(v -> enableEditText(profilePhone));


        patientUidTextView.setOnLongClickListener(v -> {
            String uidText = patientUidTextView.getText().toString().replace("UID: ", "").trim();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UID", uidText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "UID copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        });

        qrScanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                String uid = result.getData().getStringExtra("doctorUid");
                if (uid != null) linkDoctorByUid(uid); // auto link after scan
            }
        });

        // Image Picker for QR image
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                decodeQRCodeFromImage(imageUri);
            }
        });

        if (currentUser != null) {
            String uid = currentUser.getUid();
            patientUidTextView.setText("UID: " + uid);
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    profileName.setText(snapshot.child("name").getValue(String.class));
                    profileEmail.setText(snapshot.child("email").getValue(String.class));
                    profilePhone.setText(snapshot.child("phone").getValue(String.class));

                    if (snapshot.hasChild("doctorUid")) {
                        String doctorUid = snapshot.child("doctorUid").getValue(String.class);
                        loadDoctorDetails(doctorUid);
                    } else {
                        linkedDoctorName.setText("No doctor linked");
                        btnLinkDoctor.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnUpdateProfile.setOnClickListener(v -> updateProfile());


        btnLinkDoctor.setOnClickListener(v -> showDoctorLinkDialog());

        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), Login_Activity.class));
            requireActivity().finish();
        });

        btnUnlinkDoctor.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Unlink Doctor")
                    .setMessage("Are you sure you want to unlink this doctor?")
                    .setPositiveButton("Yes", (dialog, which) -> unlinkDoctor())
                    .setNegativeButton("No", null)
                    .show();
        });

        aboutUs.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("About CareBridge")
                    .setMessage("🩺 **CareBridge** is a smart health companion app that ensures patients never miss their medicines.\n\n" +
            "💊 **Core Features:**\n" +
                    "• Sends timely medicine reminders to patients\n" +
                    "• If the patient doesn’t respond, alerts are sent to their family\n" +
                    "• If the reminder is still ignored, it notifies the doctor for follow-up\n\n" +
                    "📅 Also includes:\n" +
                    "• Appointment booking\n" +
                    "• Real-time chat with doctors\n" +
                    "• Personal health dashboard\n\n" +
                    "👨‍💻 Developed by:\n" +
                    "• Suhani\n" +
                    "• Bhavishya\n" +
                    "• Yash\n" +
                    "• Akash\n" +
                    "• Harsh")
                    .setPositiveButton("OK", null)
                    .show();
        });

        return view;
    }

    private void disableEditText(EditText editText) {
        editText.setCursorVisible(false);
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
    }

    private void enableEditText(EditText editText) {
        editText.setCursorVisible(true);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }


    private void updateProfile() {
        String name = profileName.getText().toString().trim();
        String email = profileEmail.getText().toString().trim();
        String phone = profilePhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedPhone = phone.replaceAll("\\s+", "");
        if (!formattedPhone.startsWith("+")) {
            formattedPhone = "+91" + formattedPhone.replaceFirst("^0*", "");
        }

        if (formattedPhone.length() < 10 || formattedPhone.length() > 15) {
            Toast.makeText(getContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalPhone = formattedPhone;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String oldPhone = snapshot.child("phone").getValue(String.class);
                if (!finalPhone.equals(oldPhone)) {
                    Intent intent = new Intent(getActivity(), OtpVerificationActivity.class);
                    intent.putExtra("name", name);
                    intent.putExtra("email", email);
                    intent.putExtra("phone", finalPhone);
                    startActivity(intent);
                } else {
                    userRef.child("name").setValue(name);
                    userRef.child("email").setValue(email);
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDoctorDetails(String doctorUid) {
        doctorRef = FirebaseDatabase.getInstance().getReference("users").child(doctorUid);
        doctorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String doctorName = snapshot.child("name").getValue(String.class);
                String doctorEmail = snapshot.child("email").getValue(String.class);

                if (doctorName != null && doctorEmail != null) {
                    linkedDoctorName.setText("Doctor: " + doctorName + "\nEmail: " + doctorEmail);
                    btnLinkDoctor.setVisibility(View.GONE);
                    btnUnlinkDoctor.setVisibility(View.VISIBLE);
                } else {
                    linkedDoctorName.setText("Doctor details unavailable");
                    btnUnlinkDoctor.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                linkedDoctorName.setText("Error loading doctor");
                btnUnlinkDoctor.setVisibility(View.GONE);
            }
        });
    }


    private void showDoctorLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Link Doctor");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_link_doctor, null);
        EditText editDoctorUid = dialogView.findViewById(R.id.editDoctorUid);
        Button btnScanQr = dialogView.findViewById(R.id.btnScanQr);
        Button btnUploadQr = dialogView.findViewById(R.id.btnUploadImage);

        builder.setView(dialogView);
        builder.setPositiveButton("Link", (dialog, which) -> {
            String uid = editDoctorUid.getText().toString().trim();
            if (!uid.isEmpty()) {
                linkDoctorByUid(uid);
            } else {
                Toast.makeText(getContext(), "UID is required", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        btnScanQr.setOnClickListener(v -> {
            Intent qrIntent = new Intent(getActivity(), QrScannerActivity.class);
            qrScanLauncher.launch(qrIntent);
            dialog.dismiss();
        });

        btnUploadQr.setOnClickListener(v -> {
            Intent imagePicker = new Intent(Intent.ACTION_GET_CONTENT);
            imagePicker.setType("image/*");
            imagePickerLauncher.launch(imagePicker);
            dialog.dismiss();
        });
    }

    private void linkDoctorByUid(String doctorUid) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(doctorUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && "doctor".equalsIgnoreCase(snapshot.child("role").getValue(String.class))) {
                    userRef.child("doctorUid").setValue(doctorUid);
                    usersRef.child(doctorUid).child("patients").child(currentUser.getUid()).setValue(true);
                    loadDoctorDetails(doctorUid);
                    Toast.makeText(getContext(), "Doctor linked successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Invalid doctor UID", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void decodeQRCodeFromImage(Uri uri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(binaryBitmap);
            String scannedUid = result.getText();
            linkDoctorByUid(scannedUid); // auto-link on scan

        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to decode QR", Toast.LENGTH_SHORT).show();
        }
    }
    private void unlinkDoctor() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("doctorUid")) {
                    String doctorUid = snapshot.child("doctorUid").getValue(String.class);

                    // Remove doctorUid from patient node
                    userRef.child("doctorUid").removeValue();

                    // Remove patient from doctor's patient list
                    DatabaseReference doctorPatientRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(doctorUid)
                            .child("patients")
                            .child(currentUser.getUid());
                    doctorPatientRef.removeValue();

                    // UI updates
                    linkedDoctorName.setText("No doctor linked");
                    btnLinkDoctor.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Doctor unlinked successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No doctor linked", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to unlink doctor", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
