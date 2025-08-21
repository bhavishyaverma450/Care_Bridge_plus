package com.example.carebridge;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash_Activity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2500;
    private TextView logoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logoText = findViewById(R.id.logoText);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logoText.startAnimation(fadeIn);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
            boolean isRemembered = prefs.getBoolean("remember", false);
            String role = prefs.getString("role", "");  // Get saved role

            Intent intent;

            if (isRemembered) {
                if ("doctor".equalsIgnoreCase(role)) {
                    intent = new Intent(Splash_Activity.this, Doctor_Home_Activity.class);
                } else if ("patient".equalsIgnoreCase(role)) {
                    intent = new Intent(Splash_Activity.this, HomeActivity.class);
                } else {
                    intent = new Intent(Splash_Activity.this, Login_Activity.class);  // fallback
                }
            } else {
                intent = new Intent(Splash_Activity.this, Login_Activity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_TIME);
    }
}
