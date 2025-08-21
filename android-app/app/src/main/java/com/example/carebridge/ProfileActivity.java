package com.example.carebridge;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Fragment fragment = new PatientProfileFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_profile, fragment)
                .commit();
    }
}
