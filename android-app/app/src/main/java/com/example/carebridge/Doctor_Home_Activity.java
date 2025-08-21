package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class Doctor_Home_Activity extends AppCompatActivity {
    private static final String TAG = "Doctor_Home_Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        // Initialize UI elements
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView == null) {
            Log.e(TAG, "bottomNavigationView is null. Check activity_doctor_home.xml for ID 'bottom_navigation'");
            Toast.makeText(this, "Navigation initialization failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not logged in, redirecting to Login_Activity");
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }

        // Load default fragment (DoctorAppointmentsFragment as home)
        try {
            Log.d(TAG, "Loading DoctorAppointmentsFragment (default)");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DoctorAppointmentsFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load DoctorAppointmentsFragment: " + e.getMessage());
            Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Bottom Navigation Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            try {
                if (itemId == R.id.nav_home) {
                    Log.d(TAG, "Switching to DoctorAppointmentsFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new DoctorAppointmentsFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    Log.d(TAG, "Switching to PatientsFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new PatientsFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Log.d(TAG, "Switching to DoctorProfileFragment");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new DoctorProfileFragment())
                            .commit();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Fragment switch failed: " + e.getMessage());
                Toast.makeText(this, "Fragment load failed", Toast.LENGTH_SHORT).show();
            }
            return false;
        });
    }
}
