package com.example.carebridge;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class RemindersActivity extends AppCompatActivity {
    private static final String TAG = "RemindersActivity";
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;
    private LinearLayout llRemindersContainer, llMedicinesContainer;
    private final List<Medicine> medicineReminders = new ArrayList<>();
    private final List<OtherReminder> otherReminders = new ArrayList<>();

    // Button references
    private Button btnSos, btnAddReminder, btnAddMedicine;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userMedicineRemindersRef;
    private DatabaseReference userOtherRemindersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting RemindersActivity onCreate");
        try {
            setContentView(R.layout.activity_reminders);
            Log.d(TAG, "Content view set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting content view", e);
            finish();
            return;
        }

        // Initialize Firebase
        try {
            mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                // User not logged in, redirect to login
                Toast.makeText(this, "Please log in to access reminders", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login_Activity.class));
                finish();
                return;
            }

            // Initialize Firebase Database references
            String userId = currentUser.getUid();
            userMedicineRemindersRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId).child("medicines"); // Changed from "medicine_reminders"
            userOtherRemindersRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId).child("other_reminders");

            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            Toast.makeText(this, "Failed to connect to database", Toast.LENGTH_SHORT).show();
        }

        // Initialize views
        try {
            btnSos = findViewById(R.id.btn_sos);
            btnAddReminder = findViewById(R.id.btn_add_reminder);
            btnAddMedicine = findViewById(R.id.btn_add_medicine);
            llRemindersContainer = findViewById(R.id.ll_reminders_container);
            llMedicinesContainer = findViewById(R.id.ll_medicines_container);
            Log.d(TAG, "Views initialized successfully");

            // Initialize navigation and FAB with improved implementation
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

            findViewById(R.id.fab_chat).setOnClickListener(v -> {
                startActivity(new Intent(RemindersActivity.this, ChatBookingActivity.class));
            });

            // Setup bottom navigation to match HomeActivity
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(RemindersActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_records) {
                    startActivity(new Intent(RemindersActivity.this, AboutUserActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_reminders) {
                    // Already on reminders page
                    return true;
                } else if (itemId == R.id.nav_vitals) {
                    startActivity(new Intent(RemindersActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(RemindersActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            });

            // Set the reminders item as selected
            bottomNavigationView.setSelectedItemId(R.id.nav_reminders);

            // Setup SOS button
            btnSos.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
                } else {
                    sendSOS();
                }
            });

            // Add keyboard visibility detection for bottom navigation (like in HomeActivity)
            View rootView = findViewById(android.R.id.content);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is visible
                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    // Keyboard is hidden
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            finish();
            return;
        }

        // Load data from Firebase
        loadMedicineRemindersFromFirebase();
        loadOtherRemindersFromFirebase();

        // Set up Add Reminder button
        try {
            btnAddReminder.setOnClickListener(v -> {
                showAddReminderDialog();
                Log.d(TAG, "Add Reminder button clicked");
            });
            Log.d(TAG, "Add Reminder button click listener set");
        } catch (Exception e) {
            Log.e(TAG, "Error setting Add Reminder button listener", e);
        }

        // Set up Add Medicine button
        try {
            btnAddMedicine.setOnClickListener(v -> {
                showAddMedicineDialog();
                Log.d(TAG, "Add Medicine button clicked");
            });
            Log.d(TAG, "Add Medicine button click listener set");
        } catch (Exception e) {
            Log.e(TAG, "Error setting Add Medicine button listener", e);
        }


        // Set up Bottom Navigation
        try {
            // Navigation is already set up earlier in the code, so we don't need this duplicate setup
            Log.d(TAG, "Bottom navigation already set up");
        } catch (Exception e) {
            Log.e(TAG, "Error with navigation", e);
        }
    }

    private void showAddReminderDialog() {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_add_reminder);

            // Set dialog to full width and adjust for keyboard
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            // Initialize dialog views
            TextInputEditText etReminderTitle = dialog.findViewById(R.id.et_medicine_name);
            etReminderTitle.setHint("Reminder Title");
            TextInputEditText etSpecialNotes = dialog.findViewById(R.id.et_special_notes);
            AutoCompleteTextView spinnerReason = dialog.findViewById(R.id.spinner_reason);
            Button btnSelectTime = dialog.findViewById(R.id.btn_select_time);
            TextView tvSelectedTime = dialog.findViewById(R.id.tv_selected_time);
            Button btnSave = dialog.findViewById(R.id.btn_save_reminder);
            ImageButton btnClose = dialog.findViewById(R.id.btn_close);

            // Set up reason dropdown
            String[] reasons = {"Appointment", "Exercise", "Meditation", "Check-up", "Other"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, reasons);
            spinnerReason.setAdapter(adapter);

            // Time selection variables
            final Calendar calendar = Calendar.getInstance();
            final int[] hour = {calendar.get(Calendar.HOUR_OF_DAY)};
            final int[] minute = {calendar.get(Calendar.MINUTE)};
            final String[] selectedTime = {"Not Set"};

            // Set up time picker
            btnSelectTime.setOnClickListener(v -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        (view, hourOfDay, minuteOfDay) -> {
                            hour[0] = hourOfDay;
                            minute[0] = minuteOfDay;
                            selectedTime[0] = formatTime(hourOfDay, minuteOfDay);
                            tvSelectedTime.setText(selectedTime[0]);
                        },
                        hour[0],
                        minute[0],
                        false);
                timePickerDialog.show();
            });

            btnSave.setOnClickListener(v -> {
                // Validate inputs
                String reminderTitle = etReminderTitle.getText() != null ? etReminderTitle.getText().toString().trim() : "";
                String specialNotes = etSpecialNotes.getText() != null ? etSpecialNotes.getText().toString().trim() : "";
                String reason = spinnerReason.getText() != null ? spinnerReason.getText().toString().trim() : "";
                String reminderTime = tvSelectedTime.getText() != null ? tvSelectedTime.getText().toString().trim() : "Not Set";

                if (reminderTitle.isEmpty()) {
                    Toast.makeText(this, "Please enter reminder title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please select a reason", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reminderTime.equals("Not Set")) {
                    Toast.makeText(this, "Please select a reminder time", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create and save the new reminder
                OtherReminder newReminder = new OtherReminder(
                        reminderTitle,
                        reason,
                        reminderTime,
                        specialNotes
                );

                // Save to Firebase
                saveOtherReminderToFirebase(newReminder);

                dialog.dismiss();
                Toast.makeText(this, "Reminder saved successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Reminder saved: " + reminderTitle + " at " + reminderTime);
            });

            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                Log.d(TAG, "Add Reminder dialog canceled");
            });

            dialog.show();
            Log.d(TAG, "Add Reminder dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing Add Reminder dialog", e);
            Toast.makeText(this, "Could not display Add Reminder dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddMedicineDialog() {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_add_medicine);

            // Set dialog to full width and adjust for keyboard
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            // Initialize dialog views
            TextInputEditText etMedicineName = dialog.findViewById(R.id.et_add_medicine_name);
            TextInputEditText etDosage = dialog.findViewById(R.id.et_dosage);
            AutoCompleteTextView spinnerDosageUnit = dialog.findViewById(R.id.spinner_dosage_unit);
            Button btnSelectTime = dialog.findViewById(R.id.btn_select_time);
            TextView tvSelectedTime = dialog.findViewById(R.id.tv_selected_time);
            TextInputEditText etMedicineDescription = dialog.findViewById(R.id.et_medicine_description);
            Button btnSave = dialog.findViewById(R.id.btn_save_medicine);
            ImageButton btnClose = dialog.findViewById(R.id.btn_close_medicine);

            // Set up dosage unit dropdown
            String[] dosageUnits = {"Tablets", "Capsules", "ml", "mg", "g", "Drops", "Teaspoons", "Tablespoons", "Injections", "Puffs", "Patches"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dosageUnits);
            spinnerDosageUnit.setAdapter(adapter);

            // Time selection variables
            final Calendar calendar = Calendar.getInstance();
            final int[] hour = {calendar.get(Calendar.HOUR_OF_DAY)};
            final int[] minute = {calendar.get(Calendar.MINUTE)};
            final String[] selectedTime = {"Not Set"};

            // Set up time picker
            btnSelectTime.setOnClickListener(v -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        (view, hourOfDay, minuteOfDay) -> {
                            hour[0] = hourOfDay;
                            minute[0] = minuteOfDay;
                            selectedTime[0] = formatTime(hourOfDay, minuteOfDay);
                            tvSelectedTime.setText(selectedTime[0]);
                        },
                        hour[0],
                        minute[0],
                        false);
                timePickerDialog.show();
            });

            btnSave.setOnClickListener(v -> {
                // Validate inputs
                String medicineName = etMedicineName.getText() != null ? etMedicineName.getText().toString().trim() : "";
                String dosage = etDosage.getText() != null ? etDosage.getText().toString().trim() : "";
                String dosageUnit = spinnerDosageUnit.getText() != null ? spinnerDosageUnit.getText().toString().trim() : "";
                String reminderTime = tvSelectedTime.getText() != null ? tvSelectedTime.getText().toString().trim() : "Not Set";
                String description = etMedicineDescription.getText() != null ? etMedicineDescription.getText().toString().trim() : "";

                if (medicineName.isEmpty()) {
                    Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dosage.isEmpty()) {
                    Toast.makeText(this, "Please enter dosage", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reminderTime.equals("Not Set")) {
                    Toast.makeText(this, "Please select a reminder time", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create and save the new medicine reminder
                Medicine newReminder = new Medicine(
                        medicineName,
                        reminderTime,
                        false, // taken
                        dosage,
                        dosageUnit,
                        description
                );

                // Save to Firebase
                saveMedicineReminderToFirebase(newReminder);

                dialog.dismiss();
                Toast.makeText(this, "Medicine reminder saved successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Medicine reminder saved: " + medicineName);
            });

            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                Log.d(TAG, "Add Medicine dialog canceled");
            });

            dialog.show();
            Log.d(TAG, "Add Medicine dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing Add Medicine dialog", e);
            Toast.makeText(this, "Could not display Add Medicine dialog", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to show edit reminder dialog
    private void showEditReminderDialog(OtherReminder reminder, int position, View reminderView) {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_add_reminder);

            // Store original reminder data for Firebase update
            final OtherReminder originalReminder = new OtherReminder(
                    reminder.getTitle(),
                    reminder.getReason(),
                    reminder.getTime(),
                    reminder.getNotes()
            );
            originalReminder.setId(reminder.getId());


            // Update dialog title
            TextView tvTitle = dialog.findViewById(R.id.tv_add_reminder_title);
            if (tvTitle != null) {
                tvTitle.setText(R.string.edit_reminder_title);
            }

            // Set dialog to full width and adjust for keyboard
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            // Initialize dialog views
            TextInputEditText etReminderTitle = dialog.findViewById(R.id.et_medicine_name);
            etReminderTitle.setHint("Reminder Title");
            TextInputEditText etSpecialNotes = dialog.findViewById(R.id.et_special_notes);
            AutoCompleteTextView spinnerReason = dialog.findViewById(R.id.spinner_reason);
            Button btnSelectTime = dialog.findViewById(R.id.btn_select_time);
            TextView tvSelectedTime = dialog.findViewById(R.id.tv_selected_time);
            Button btnSave = dialog.findViewById(R.id.btn_save_reminder);
            ImageButton btnClose = dialog.findViewById(R.id.btn_close);

            // Fill dialog with existing reminder data
            etReminderTitle.setText(reminder.getTitle());
            etSpecialNotes.setText(reminder.getNotes());

            // Set up reason dropdown
            String[] reasons = {"Appointment", "Exercise", "Meditation", "Check-up", "Other"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, reasons);
            spinnerReason.setAdapter(adapter);
            spinnerReason.setText(reminder.getReason(), false);


            // Time selection variables - parse existing time
            String reminderTime = reminder.getTime();
            final Calendar calendar = Calendar.getInstance();
            final int[] hour = {calendar.get(Calendar.HOUR_OF_DAY)};
            final int[] minute = {calendar.get(Calendar.MINUTE)};

            // Set the initial selected time text
            tvSelectedTime.setText(reminderTime);

            // Set up time picker
            btnSelectTime.setOnClickListener(v -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        (view, hourOfDay, minuteOfDay) -> {
                            hour[0] = hourOfDay;
                            minute[0] = minuteOfDay;
                            String newTime = formatTime(hourOfDay, minuteOfDay);
                            tvSelectedTime.setText(newTime);
                        },
                        hour[0],
                        minute[0],
                        false);
                timePickerDialog.show();
            });

            btnSave.setOnClickListener(v -> {
                // Validate inputs
                String title = etReminderTitle.getText() != null ? etReminderTitle.getText().toString().trim() : "";
                String specialNotes = etSpecialNotes.getText() != null ? etSpecialNotes.getText().toString().trim() : "";
                String reason = spinnerReason.getText() != null ? spinnerReason.getText().toString().trim() : "";
                String updatedReminderTime = tvSelectedTime.getText() != null ? tvSelectedTime.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please enter reminder title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please select a reason", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update the reminder object
                reminder.setTitle(title);
                reminder.setNotes(specialNotes);
                reminder.setReason(reason);
                reminder.setTime(updatedReminderTime);

                // Update the UI
                TextView tvReminderTitle = reminderView.findViewById(R.id.tv_medicine_name);
                TextView tvReason = reminderView.findViewById(R.id.tv_reason);
                TextView tvTime = reminderView.findViewById(R.id.tv_time);

                tvReminderTitle.setText(title);
                tvReason.setText(reason);
                tvTime.setText(updatedReminderTime);

                // Update in Firebase
                updateOtherReminderInFirebase(reminder);

                dialog.dismiss();
                Toast.makeText(this, "Reminder updated successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Reminder updated: " + title + " at " + updatedReminderTime);
            });

            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                Log.d(TAG, "Edit Reminder dialog canceled");
            });

            dialog.show();
            Log.d(TAG, "Edit Reminder dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing Edit Reminder dialog", e);
            Toast.makeText(this, "Could not display Edit Reminder dialog", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to update tags for all medicine views after deletion
    private void updateMedicineViewTags() {
        for (int i = 0; i < llMedicinesContainer.getChildCount(); i++) {
            View childView = llMedicinesContainer.getChildAt(i);
            // Skip if the view is a Button (like the Add Medicine button)
            if (childView instanceof Button) continue;

            // Process only medicine views that have tags
            if (childView.getTag() != null) {
                // Get current position and update it
                for (int j = 0; j < medicineReminders.size(); j++) {
                    Medicine reminder = medicineReminders.get(j);
                    TextView nameView = childView.findViewById(R.id.tv_medicine_name);
                    if (nameView != null && nameView.getText().toString().equals(reminder.getName())) {
                        childView.setTag(j);
                        break;
                    }
                }
            }
        }
    }

    // Method to show edit medicine dialog
    private void showEditMedicineReminderDialog(Medicine reminder, int position, View medicineView) {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_add_medicine);

            // Store original medicine name for Firebase update
            final Medicine originalReminder = new Medicine(
                    reminder.getName(),
                    reminder.getTime(),
                    reminder.isTaken(),
                    reminder.getDosage(),
                    reminder.getDosageUnit(),
                    reminder.getSpecialNotes()
            );
            originalReminder.setId(reminder.getId());

            // Update dialog title
            TextView tvTitle = dialog.findViewById(R.id.tv_add_medicine_title);
            tvTitle.setText("Edit Medicine Reminder");

            // Set dialog to full width and adjust for keyboard
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            // Initialize dialog views
            TextInputEditText etMedicineName = dialog.findViewById(R.id.et_add_medicine_name);
            TextInputEditText etDosage = dialog.findViewById(R.id.et_dosage);
            AutoCompleteTextView spinnerDosageUnit = dialog.findViewById(R.id.spinner_dosage_unit);
            Button btnSelectTime = dialog.findViewById(R.id.btn_select_time);
            TextView tvSelectedTime = dialog.findViewById(R.id.tv_selected_time);
            TextInputEditText etMedicineDescription = dialog.findViewById(R.id.et_medicine_description);
            Button btnSave = dialog.findViewById(R.id.btn_save_medicine);
            ImageButton btnClose = dialog.findViewById(R.id.btn_close_medicine);

            // Fill dialog with existing medicine data
            etMedicineName.setText(reminder.getName());
            etDosage.setText(reminder.getDosage());
            etMedicineDescription.setText(reminder.getSpecialNotes());


            // Set up dosage unit dropdown
            String[] dosageUnits = {"Tablets", "Capsules", "ml", "mg", "g", "Drops", "Teaspoons", "Tablespoons", "Injections", "Puffs", "Patches"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dosageUnits);
            spinnerDosageUnit.setAdapter(adapter);
            spinnerDosageUnit.setText(reminder.getDosageUnit(), false);

            // Time selection variables
            String reminderTime = reminder.getTime();
            final Calendar calendar = Calendar.getInstance();
            final int[] hour = {calendar.get(Calendar.HOUR_OF_DAY)};
            final int[] minute = {calendar.get(Calendar.MINUTE)};

            tvSelectedTime.setText(reminderTime);

            // Set up time picker
            btnSelectTime.setOnClickListener(v -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        (view, hourOfDay, minuteOfDay) -> {
                            hour[0] = hourOfDay;
                            minute[0] = minuteOfDay;
                            String newTime = formatTime(hourOfDay, minuteOfDay);
                            tvSelectedTime.setText(newTime);
                        },
                        hour[0],
                        minute[0],
                        false);
                timePickerDialog.show();
            });


            btnSave.setOnClickListener(v -> {
                // Validate inputs
                String medicineName = etMedicineName.getText() != null ? etMedicineName.getText().toString().trim() : "";
                String dosage = etDosage.getText() != null ? etDosage.getText().toString().trim() : "";
                String dosageUnit = spinnerDosageUnit.getText() != null ? spinnerDosageUnit.getText().toString().trim() : "";
                String updatedReminderTime = tvSelectedTime.getText() != null ? tvSelectedTime.getText().toString().trim() : "";
                String description = etMedicineDescription.getText() != null ? etMedicineDescription.getText().toString().trim() : "";

                if (medicineName.isEmpty()) {
                    Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dosage.isEmpty()) {
                    Toast.makeText(this, "Please enter dosage", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update the medicine object
                reminder.setName(medicineName);
                reminder.setDosage(dosage);
                reminder.setDosageUnit(dosageUnit);
                reminder.setTime(updatedReminderTime);
                reminder.setSpecialNotes(description);

                // Update the UI
                TextView tvMedicineName = medicineView.findViewById(R.id.tv_medicine_name);
                TextView tvDosage = medicineView.findViewById(R.id.tv_dosage);
                TextView tvTime = medicineView.findViewById(R.id.tv_time);

                tvMedicineName.setText(medicineName);
                tvDosage.setText(String.format("%s %s", dosage, dosageUnit));
                tvTime.setText(updatedReminderTime);

                // Update medicine in Firebase
                updateMedicineReminderInFirebase(reminder);

                dialog.dismiss();
                Toast.makeText(this, "Medicine reminder updated successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Medicine reminder updated: " + medicineName);
            });

            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                Log.d(TAG, "Edit Medicine dialog canceled");
            });

            dialog.show();
            Log.d(TAG, "Edit Medicine dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing Edit Medicine dialog", e);
            Toast.makeText(this, "Could not display Edit Medicine dialog", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to format time
    private String formatTime(int hour, int minute) {
        String amPm = hour < 12 ? "AM" : "PM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm);
    }

    // Helper method to add reminder to the UI
    private void addOtherReminderToUI(OtherReminder reminder) {
        // Use the parent view for proper layout params
        View reminderView = getLayoutInflater().inflate(R.layout.item_other_reminder, llRemindersContainer, false);

        // Configure the view
        TextView tvReminderTitle = reminderView.findViewById(R.id.tv_medicine_name);
        TextView tvReason = reminderView.findViewById(R.id.tv_reason);
        TextView tvTime = reminderView.findViewById(R.id.tv_time);

        tvReminderTitle.setText(reminder.getTitle());
        tvReason.setText(reminder.getReason());
        tvTime.setText(reminder.getTime());

        // Add margins to the view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 16); // left, top, right, bottom margins
        reminderView.setLayoutParams(params);

        // Store the reminder's position in the list as a tag on the view
        int position = otherReminders.indexOf(reminder);
        reminderView.setTag(position);

        ImageButton btnEdit = reminderView.findViewById(R.id.btn_edit_reminder);
        btnEdit.setOnClickListener(v -> {
            int reminderPosition = (int) reminderView.getTag();
            if (reminderPosition >= 0 && reminderPosition < otherReminders.size()) {
                showEditReminderDialog(otherReminders.get(reminderPosition), reminderPosition, reminderView);
            }
        });

        // Add click listener for delete button
        ImageButton btnDelete = reminderView.findViewById(R.id.btn_delete_reminder);
        btnDelete.setOnClickListener(v -> {
            int reminderPosition = (int) reminderView.getTag();
            if (reminderPosition >= 0 && reminderPosition < otherReminders.size()) {
                deleteOtherReminder(reminderPosition, reminderView);
            }
        });

        // Get reference to Add Reminder button
        Button btnAddReminder = findViewById(R.id.btn_add_reminder);

        // Add new reminder view right before the Add Reminder button
        int addButtonIndex = -1;
        for (int i = 0; i < llRemindersContainer.getChildCount(); i++) {
            if (llRemindersContainer.getChildAt(i) == btnAddReminder) {
                addButtonIndex = i;
                break;
            }
        }

        if (addButtonIndex != -1) {
            llRemindersContainer.addView(reminderView, addButtonIndex);
        } else {
            // Fallback - add it right before the last element (which should be the button)
            llRemindersContainer.addView(reminderView, Math.max(0, llRemindersContainer.getChildCount() - 1));
        }
    }

    // Helper method to add medicine to the UI
    private void addMedicineReminderToUI(Medicine reminder) {
        View medicineView = getLayoutInflater().inflate(R.layout.item_medicine_reminder, llMedicinesContainer, false);

        TextView tvMedicineName = medicineView.findViewById(R.id.tv_medicine_name);
        TextView tvDosage = medicineView.findViewById(R.id.tv_dosage);
        TextView tvTime = medicineView.findViewById(R.id.tv_time);

        tvMedicineName.setText(reminder.getName());
        tvDosage.setText(String.format("%s %s", reminder.getDosage(), reminder.getDosageUnit()));
        tvTime.setText(reminder.getTime());

        // Add margins to the view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 16); // left, top, right, bottom margins
        medicineView.setLayoutParams(params);

        // Store the medicine's position in the list as a tag on the view
        int position = medicineReminders.indexOf(reminder);
        medicineView.setTag(position);

        // Add click listener for edit button
        ImageButton btnEdit = medicineView.findViewById(R.id.btn_edit_reminder);
        btnEdit.setOnClickListener(v -> {
            int medicinePosition = (int) medicineView.getTag();
            if (medicinePosition >= 0 && medicinePosition < medicineReminders.size()) {
                showEditMedicineReminderDialog(medicineReminders.get(medicinePosition), medicinePosition, medicineView);
            }
        });

        // Add click listener for delete button
        ImageButton btnDelete = medicineView.findViewById(R.id.btn_delete_reminder);
        btnDelete.setOnClickListener(v -> {
            int medicinePosition = (int) medicineView.getTag();
            if (medicinePosition >= 0 && medicinePosition < medicineReminders.size()) {
                deleteMedicineReminder(medicinePosition, medicineView);
            }
        });

        // Get reference to Add Medicine button
        Button btnAddMedicine = findViewById(R.id.btn_add_medicine);

        // Add new medicine view right before the Add Medicine button
        int addButtonIndex = -1;
        for (int i = 0; i < llMedicinesContainer.getChildCount(); i++) {
            if (llMedicinesContainer.getChildAt(i) == btnAddMedicine) {
                addButtonIndex = i;
                break;
            }
        }

        if (addButtonIndex != -1) {
            llMedicinesContainer.addView(medicineView, addButtonIndex);
        } else {
            // Fallback - add it right before the last element (which should be the button)
            llMedicinesContainer.addView(medicineView, Math.max(0, llMedicinesContainer.getChildCount() - 1));
        }
    }

    // Method to delete a medicine from the list and UI
    private void deleteMedicineReminder(int position, View medicineView) {
        try {
            // Get the medicine to be deleted
            Medicine reminder = medicineReminders.get(position);
            String medicineName = reminder.getName();

            // Create a confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete Medicine Reminder");
            builder.setMessage("Are you sure you want to delete the reminder for " + medicineName + "?");

            // Add the buttons
            builder.setPositiveButton("Delete", (dialog, which) -> {
                // Remove from the data list
                medicineReminders.remove(position);

                // Remove from the UI
                llMedicinesContainer.removeView(medicineView);

                // Delete from Firebase
                deleteMedicineReminderFromFirebase(reminder);

                // Update tags for remaining medicine views
                updateMedicineViewTags();

                Toast.makeText(this, "Reminder for " + medicineName + " has been deleted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Medicine reminder deleted: " + medicineName);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
                Log.d(TAG, "Medicine reminder deletion canceled for: " + medicineName);
            });

            // Create and show the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting medicine reminder", e);
            Toast.makeText(this, "Could not delete medicine reminder", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to delete a reminder from the list and UI
    private void deleteOtherReminder(int position, View reminderView) {
        try {
            // Get the reminder to be deleted
            OtherReminder reminder = otherReminders.get(position);
            String reminderName = reminder.getTitle();

            // Create a confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete Reminder");
            builder.setMessage("Are you sure you want to delete the reminder for " + reminderName + "?");

            // Add the buttons
            builder.setPositiveButton("Delete", (dialog, which) -> {
                // Remove from the data list
                otherReminders.remove(position);

                // Remove from the UI
                llRemindersContainer.removeView(reminderView);

                // Delete from Firebase
                deleteOtherReminderFromFirebase(reminder);

                Toast.makeText(this, "Reminder for " + reminderName + " has been deleted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Reminder deleted: " + reminderName);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
                Log.d(TAG, "Reminder deletion canceled for: " + reminderName);
            });

            // Create and show the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting reminder", e);
            Toast.makeText(this, "Could not delete reminder", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to load reminders from Firebase
    private void loadMedicineRemindersFromFirebase() {
        try {
            userMedicineRemindersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    medicineReminders.clear();
                    // Remove all views except the button
                    for (int i = llMedicinesContainer.getChildCount() - 2; i >= 0; i--) {
                        View view = llMedicinesContainer.getChildAt(i);
                        if (view.getId() != R.id.btn_add_medicine) {
                            llMedicinesContainer.removeViewAt(i);
                        }
                    }

                    // Remove "No reminders" message if it exists
                    View noRemindersView = llMedicinesContainer.findViewWithTag("no_reminders_medicine");
                    if (noRemindersView != null) {
                        llMedicinesContainer.removeView(noRemindersView);
                    }

                    if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Medicine reminder = snapshot.getValue(Medicine.class);
                            if (reminder != null) {
                                reminder.setId(snapshot.getKey());
                                medicineReminders.add(reminder);
                                addMedicineReminderToUI(reminder);
                            }
                        }
                    } else {
                        // Show "No medicine reminders" message
                        TextView tvNoReminders = new TextView(RemindersActivity.this);
                        tvNoReminders.setText("No medicine reminders");
                        tvNoReminders.setTag("no_reminders_medicine");
                        tvNoReminders.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0, 32, 0, 32);
                        tvNoReminders.setLayoutParams(params);
                        int buttonIndex = llMedicinesContainer.indexOfChild(findViewById(R.id.btn_add_medicine));
                        llMedicinesContainer.addView(tvNoReminders, buttonIndex);
                    }
                    Log.d(TAG, "Medicine reminders loaded from Firebase: " + medicineReminders.size());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load medicine reminders from Firebase", databaseError.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading medicine reminders from Firebase", e);
        }
    }

    private void loadOtherRemindersFromFirebase() {
        try {
            userOtherRemindersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    otherReminders.clear();
                    // Remove all views except the button
                    for (int i = llRemindersContainer.getChildCount() - 2; i >= 0; i--) {
                        View view = llRemindersContainer.getChildAt(i);
                        if (view.getId() != R.id.btn_add_reminder) {
                            llRemindersContainer.removeViewAt(i);
                        }
                    }

                    // Remove "No reminders" message if it exists
                    View noRemindersView = llRemindersContainer.findViewWithTag("no_reminders_other");
                    if (noRemindersView != null) {
                        llRemindersContainer.removeView(noRemindersView);
                    }

                    if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            OtherReminder reminder = snapshot.getValue(OtherReminder.class);
                            if (reminder != null) {
                                reminder.setId(snapshot.getKey());
                                otherReminders.add(reminder);
                                addOtherReminderToUI(reminder);
                            }
                        }
                    } else {
                        // Show "No other reminders" message
                        TextView tvNoReminders = new TextView(RemindersActivity.this);
                        tvNoReminders.setText("No other reminders");
                        tvNoReminders.setTag("no_reminders_other");
                        tvNoReminders.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0, 32, 0, 32);
                        tvNoReminders.setLayoutParams(params);
                        int buttonIndex = llRemindersContainer.indexOfChild(findViewById(R.id.btn_add_reminder));
                        llRemindersContainer.addView(tvNoReminders, buttonIndex);
                    }
                    Log.d(TAG, "Other reminders loaded from Firebase: " + otherReminders.size());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load other reminders from Firebase", databaseError.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading other reminders from Firebase", e);
        }
    }


    private void saveMedicineReminderToFirebase(Medicine reminder) {
        try {
            userMedicineRemindersRef.push().setValue(reminder)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Medicine reminder saved to Firebase successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save medicine reminder to Firebase", e));
        } catch (Exception e) {
            Log.e(TAG, "Error saving medicine reminder to Firebase", e);
        }
    }

    private void saveOtherReminderToFirebase(OtherReminder reminder) {
        try {
            userOtherRemindersRef.push().setValue(reminder)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Other reminder saved to Firebase successfully");
                        // Get the key (ID) of the newly created reminder
                        userOtherRemindersRef.orderByChild("title").equalTo(reminder.getTitle())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            OtherReminder savedReminder = ds.getValue(OtherReminder.class);
                                            if (savedReminder != null &&
                                                    savedReminder.getTitle().equals(reminder.getTitle()) &&
                                                    savedReminder.getTime().equals(reminder.getTime())) {

                                                String reminderId = ds.getKey();
                                                reminder.setId(reminderId);

                                                // Schedule the reminder notification
                                                scheduleOtherReminder(reminder);
                                                break;
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Failed to retrieve saved reminder: " + error.getMessage());
                                    }
                                });
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save other reminder to Firebase", e));
        } catch (Exception e) {
            Log.e(TAG, "Error saving other reminder to Firebase", e);
        }
    }

    private void scheduleOtherReminder(OtherReminder reminder) {
        try {
            // Parse the time string to set the alarm
            String timeStr = reminder.getTime();
            if (timeStr == null || timeStr.isEmpty()) {
                Log.e(TAG, "Cannot schedule reminder: time is null or empty");
                return;
            }

            // Get the alarm manager
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            // Parse time in the correct format (e.g. "9:30 AM")
            Calendar calendar = Calendar.getInstance();
            try {
                // Parse the time string which is in format like "9:30 AM"
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
                java.util.Date date = sdf.parse(timeStr);

                if (date != null) {
                    // Transfer the hour and minute to our calendar
                    Calendar timeCal = Calendar.getInstance();
                    timeCal.setTime(date);

                    // Set the time for today
                    calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    // If the time is in the past, schedule for tomorrow
                    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    Log.d(TAG, "Scheduling reminder for: " + calendar.getTime().toString());
                } else {
                    Log.e(TAG, "Failed to parse time string: " + timeStr);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse reminder time: " + e.getMessage());
                return;
            }

            // Create intent for the broadcast receiver
            Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
            intent.putExtra("reminderType", "other");
            intent.putExtra("reminderId", reminder.getId());
            intent.putExtra("title", reminder.getTitle());
            intent.putExtra("reason", reminder.getReason());
            intent.putExtra("notes", reminder.getNotes());

            // Create unique ID for this reminder
            int requestCode = ("other_" + reminder.getId()).hashCode();

            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule the alarm
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "Other reminder scheduled for " + timeStr + ": " + reminder.getTitle());
                    } else {
                        Log.w(TAG, "Cannot schedule exact alarms, permission not granted");
                        Toast.makeText(this, "Permission needed to schedule exact alarms", Toast.LENGTH_SHORT).show();
                        Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(permissionIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "Other reminder scheduled for " + timeStr + ": " + reminder.getTitle());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when scheduling alarm: " + e.getMessage());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Toast.makeText(this, "Permission needed to schedule reminders", Toast.LENGTH_SHORT).show();
                    Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(permissionIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule other reminder: " + e.getMessage());
        }
    }

    private void updateOtherReminderInFirebase(OtherReminder reminder) {
        try {
            if (reminder.getId() == null) {
                Log.e(TAG, "Cannot update reminder: id is null");
                return;
            }

            // First cancel the existing alarm
            cancelOtherReminderAlarm(reminder.getId());

            // Update the reminder in Firebase
            userOtherRemindersRef.child(reminder.getId()).setValue(reminder)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Other reminder updated successfully in Firebase");
                        // Schedule the updated reminder
                        scheduleOtherReminder(reminder);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update other reminder in Firebase", e));
        } catch (Exception e) {
            Log.e(TAG, "Error updating other reminder in Firebase", e);
        }
    }

    private void cancelOtherReminderAlarm(String reminderId) {
        try {
            // Create intent similar to the one used to create the alarm
            Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
            // Create unique ID for this reminder
            int requestCode = ("other_" + reminderId).hashCode();

            // Create pending intent with the same parameters used when creating the alarm
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Get alarm manager and cancel the alarm
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Canceled alarm for other reminder: " + reminderId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error canceling other reminder alarm", e);
        }
    }

    private void updateMedicineReminderInFirebase(Medicine reminder) {
        try {
            if (reminder.getId() == null) {
                Log.e(TAG, "Cannot update medicine reminder: id is null");
                return;
            }

            // First cancel the existing alarm
            cancelMedicineReminderAlarm(reminder.getId());

            // Update the medicine reminder in Firebase
            userMedicineRemindersRef.child(reminder.getId()).setValue(reminder)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Medicine reminder updated successfully in Firebase");
                        // Re-schedule the medicine reminder in HomeActivity
                        // This is handled automatically when data changes in Firebase
                        // But we need to make sure the HomeActivity reschedules it with proper data
                        rescheduleUpdatedMedicineReminder(reminder);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update medicine reminder in Firebase", e));
        } catch (Exception e) {
            Log.e(TAG, "Error updating medicine reminder in Firebase", e);
        }
    }

    private void cancelMedicineReminderAlarm(String medicineId) {
        try {
            // Create intent similar to the one used to create the alarm
            Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
            // Create unique ID for this medicine reminder
            int requestCode = medicineId.hashCode();

            // Create pending intent with the same parameters used when creating the alarm
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Also cancel the "missed check" alarm
            Intent missedIntent = new Intent(this, ReminderBroadcastReceiver.class);
            missedIntent.putExtra("isMissedCheck", true);
            PendingIntent missedPendingIntent = PendingIntent.getBroadcast(
                    this,
                    (medicineId + "_missed").hashCode(),
                    missedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Get alarm manager and cancel the alarms
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                alarmManager.cancel(missedPendingIntent);
                Log.d(TAG, "Canceled alarms for medicine reminder: " + medicineId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error canceling medicine reminder alarms", e);
        }
    }

    private void rescheduleUpdatedMedicineReminder(Medicine reminder) {
        try {
            // Parse the time string to set the alarm
            String timeStr = reminder.getTime();
            if (timeStr == null || timeStr.isEmpty()) {
                Log.e(TAG, "Cannot schedule medicine reminder: time is null or empty");
                return;
            }

            // Get the alarm manager
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            // Parse time and set calendar
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Calendar calendar = Calendar.getInstance();
            try {
                calendar.setTime(sdf.parse(timeStr));
                calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                // If the time is in the past, schedule for tomorrow
                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse medicine reminder time: " + e.getMessage());
                return;
            }

            // Create intent for the broadcast receiver
            Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
            intent.putExtra("reminderType", "medicine");
            intent.putExtra("medicineId", reminder.getId());
            intent.putExtra("medicineName", reminder.getName());
            intent.putExtra("userId", mAuth.getCurrentUser().getUid());
            intent.putExtra("dosage", reminder.getDosage());
            intent.putExtra("dosageUnit", reminder.getDosageUnit());

            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    reminder.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule the alarm
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );

                        // Schedule missed dose check after 30 minutes
                        Calendar missedCalendar = (Calendar) calendar.clone();
                        missedCalendar.add(Calendar.MINUTE, 30);
                        Intent missedIntent = new Intent(this, ReminderBroadcastReceiver.class);
                        missedIntent.putExtra("medicineId", reminder.getId());
                        missedIntent.putExtra("medicineName", reminder.getName());
                        missedIntent.putExtra("userId", mAuth.getCurrentUser().getUid());
                        missedIntent.putExtra("isMissedCheck", true);
                        PendingIntent missedPendingIntent = PendingIntent.getBroadcast(
                                this,
                                (reminder.getId() + "_missed").hashCode(),
                                missedIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                missedCalendar.getTimeInMillis(),
                                missedPendingIntent
                        );

                        Log.d(TAG, "Medicine reminder scheduled for " + timeStr + ": " + reminder.getName());
                    } else {
                        Log.w(TAG, "Cannot schedule exact alarms, permission not granted");
                        Toast.makeText(this, "Permission needed to schedule exact alarms", Toast.LENGTH_SHORT).show();
                        Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(permissionIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );

                    // Schedule missed dose check after 30 minutes
                    Calendar missedCalendar = (Calendar) calendar.clone();
                    missedCalendar.add(Calendar.MINUTE, 30);
                    Intent missedIntent = new Intent(this, ReminderBroadcastReceiver.class);
                    missedIntent.putExtra("medicineId", reminder.getId());
                    missedIntent.putExtra("medicineName", reminder.getName());
                    missedIntent.putExtra("userId", mAuth.getCurrentUser().getUid());
                    missedIntent.putExtra("isMissedCheck", true);
                    PendingIntent missedPendingIntent = PendingIntent.getBroadcast(
                            this,
                            (reminder.getId() + "_missed").hashCode(),
                            missedIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            missedCalendar.getTimeInMillis(),
                            missedPendingIntent
                    );

                    Log.d(TAG, "Medicine reminder scheduled for " + timeStr + ": " + reminder.getName());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when scheduling alarm: " + e.getMessage());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Toast.makeText(this, "Permission needed to schedule reminders", Toast.LENGTH_SHORT).show();
                    Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(permissionIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reschedule medicine reminder: " + e.getMessage());
        }
    }

    private void deleteOtherReminderFromFirebase(OtherReminder reminder) {
        try {
            if (reminder.getId() == null) {
                Log.e(TAG, "Cannot delete reminder: id is null");
                return;
            }

            // First cancel the alarm
            cancelOtherReminderAlarm(reminder.getId());

            // Delete from Firebase
            userOtherRemindersRef.child(reminder.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Other reminder deleted successfully from Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete other reminder from Firebase", e));
        } catch (Exception e) {
            Log.e(TAG, "Error deleting other reminder from Firebase", e);
        }
    }

    private void deleteMedicineReminderFromFirebase(Medicine reminder) {
        try {
            if (reminder.getId() == null) {
                Log.e(TAG, "Cannot delete medicine reminder: id is null");
                return;
            }

            // First cancel the alarms
            cancelMedicineReminderAlarm(reminder.getId());

            // Delete from Firebase
            userMedicineRemindersRef.child(reminder.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Medicine reminder deleted successfully from Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete medicine reminder from Firebase", e));
        } catch (Exception e) {
            Log.e(TAG, "Error deleting medicine reminder from Firebase", e);
        }
    }

    private void sendSOS() {
        try {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String contact1Phone = snapshot.child("emergencyContact1").getValue(String.class);
                    String contact2Phone = snapshot.child("emergencyContact2").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String message = name != null ?
                            name + " has triggered an SOS alert!" :
                            "SOS alert from CareBridge user!";

                    SmsManager smsManager = SmsManager.getDefault();
                    try {
                        if (contact1Phone != null && !contact1Phone.isEmpty()) {
                            smsManager.sendTextMessage(contact1Phone, null, message, null, null);
                        }
                        if (contact2Phone != null && !contact2Phone.isEmpty()) {
                            smsManager.sendTextMessage(contact2Phone, null, message, null, null);
                        }
                        Toast.makeText(RemindersActivity.this, "SOS SMS sent", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send SOS SMS: " + e.getMessage());
                        Toast.makeText(RemindersActivity.this, "Failed to send SOS SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch emergency contacts: " + error.getMessage());
                    Toast.makeText(RemindersActivity.this, "Failed to fetch contacts", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending SOS", e);
            Toast.makeText(this, "Error sending SOS", Toast.LENGTH_SHORT).show();
        }
    }
}
