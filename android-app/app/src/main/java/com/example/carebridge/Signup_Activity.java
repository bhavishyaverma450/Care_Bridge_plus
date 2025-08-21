package com.example.carebridge;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Signup_Activity extends AppCompatActivity {
    private EditText fullName, signupEmail, signupPassword, confirmPassword;
    private Button signupButton;
    private TextView goToLogin;
    private ImageView signupToggle;
    private Switch roleSwitch;
    private TextView rolePatient, roleDoctor;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        fullName = findViewById(R.id.fullName);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        signupButton = findViewById(R.id.signupButton);
        goToLogin = findViewById(R.id.goToLogin);
        signupToggle = findViewById(R.id.signupToggle);
        roleSwitch = findViewById(R.id.roleSwitch);
        rolePatient = findViewById(R.id.rolePatient);
        roleDoctor = findViewById(R.id.roleDoctor);

        signupToggle.setOnClickListener(v -> {
            int inputType = isPasswordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            signupPassword.setInputType(inputType);
            confirmPassword.setInputType(inputType);
            signupToggle.setImageResource(isPasswordVisible ? R.drawable.ic_eye_closedd : R.drawable.ic_eye_opendd);
            isPasswordVisible = !isPasswordVisible;
            signupPassword.setSelection(signupPassword.getText().length());
            confirmPassword.setSelection(confirmPassword.getText().length());
        });

        signupButton.setOnClickListener(v -> {
            String name = fullName.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String pass = signupPassword.getText().toString().trim();
            String confirm = confirmPassword.getText().toString().trim();
            String role = roleSwitch.isChecked() ? "doctor" : "patient";

            if (TextUtils.isEmpty(name)) {
                fullName.setError("Name is required");
                return;
            }
            if (TextUtils.isEmpty(email) || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                signupEmail.setError("Valid email is required");
                return;
            }
            if (!pass.equals(confirm)) {
                confirmPassword.setError("Passwords do not match");
                return;
            }
            if (!isValidPassword(pass)) {
                Toast.makeText(this, "Password must have 6+ chars, 1 uppercase, 1 special char", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String uid = user.getUid();
                            DatabaseReference ref = userRef.child(uid);
                            ref.child("name").setValue(name);
                            ref.child("email").setValue(email);
                            ref.child("role").setValue(role);

                            SharedPreferences.Editor editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit();
                            editor.putBoolean("remember", true);
                            editor.putString("role", role);
                            editor.apply();

                            Intent intent = role.equalsIgnoreCase("patient") ?
                                    new Intent(Signup_Activity.this, HomeActivity.class) :
                                    new Intent(Signup_Activity.this, Doctor_Home_Activity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(Signup_Activity.this, Login_Activity.class));
            finish();
        });
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[!@#$%^&*+=?-].*");
    }
}