package com.example.carebridge;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class Login_Activity extends AppCompatActivity {
    private EditText emailOrPhone, loginPassword;
    private CheckBox rememberMe;
    private Button loginButton;
    private ImageView loginToggle;
    private TextView goToSignup;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailOrPhone = findViewById(R.id.emailOrPhone);
        loginPassword = findViewById(R.id.loginPassword);
        rememberMe = findViewById(R.id.rememberMe);
        loginButton = findViewById(R.id.loginButton);
        loginToggle = findViewById(R.id.loginToggle);
        goToSignup = findViewById(R.id.goToSignup);
        mAuth = FirebaseAuth.getInstance();

        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        boolean isRemembered = prefs.getBoolean("remember", false);

        if (mAuth.getCurrentUser() != null && isRemembered) {
            checkUserRoleAndRedirect(mAuth.getCurrentUser());
            return;
        }

        loginButton.setOnClickListener(v -> {
            String input = emailOrPhone.getText().toString().trim();
            String loginPass = loginPassword.getText().toString().trim();

            if (TextUtils.isEmpty(input)) {
                emailOrPhone.setError("Email or phone is required");
                return;
            }
            if (TextUtils.isEmpty(loginPass)) {
                loginPassword.setError("Password is required");
                return;
            }

            if (input.contains("@")) {
                // Email login
                mAuth.signInWithEmailAndPassword(input, loginPass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                checkUserRoleAndRedirect(mAuth.getCurrentUser());
                            } else {
                                Toast.makeText(Login_Activity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                // Phone login
                String formattedPhone = input.replaceAll("\\s+", "");
                if (!formattedPhone.startsWith("+")) {
                    formattedPhone = "+91" + formattedPhone.replaceFirst("^0*", "");
                }
                userRef = FirebaseDatabase.getInstance().getReference("users");
                userRef.orderByChild("phone").equalTo(formattedPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String email = ds.child("email").getValue(String.class);
                                if (email != null) {
                                    mAuth.signInWithEmailAndPassword(email, loginPass)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    checkUserRoleAndRedirect(mAuth.getCurrentUser());
                                                } else {
                                                    Toast.makeText(Login_Activity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                            }
                        } else {
                            Toast.makeText(Login_Activity.this, "Phone number not registered", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Login_Activity.this, "Failed to fetch user", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        loginToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                loginPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            isPasswordVisible = !isPasswordVisible;
            loginPassword.setSelection(loginPassword.getText().length());
        });

        goToSignup.setOnClickListener(v -> {
            startActivity(new Intent(Login_Activity.this, Signup_Activity.class));
            finish();
        });
    }

    private void checkUserRoleAndRedirect(FirebaseUser user) {
        String uid = user.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                if (role != null) {
                    SharedPreferences.Editor editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("remember", rememberMe.isChecked());
                    editor.putString("role", role);
                    editor.apply();

                    Intent intent = "doctor".equalsIgnoreCase(role) ?
                            new Intent(Login_Activity.this, Doctor_Home_Activity.class) :
                            new Intent(Login_Activity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Login_Activity.this, "Role not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Login_Activity.this, "Failed to fetch role", Toast.LENGTH_SHORT).show();
            }
        });
    }
}